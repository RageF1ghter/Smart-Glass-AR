package com.ml.quaterion.facenetdetection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ml.quaterion.facenetdetection.model.FaceNetModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

// Analyser class to process frames and produce detections.
class FrameAnalyser( private var context: Context ,
                     private var boundingBoxOverlay: BoundingBoxOverlay ,
                     private var model: FaceNetModel
                     ) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
            .setMinFaceSize(0.2F)
            .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    private val nameScoreHashmap = HashMap<String,ArrayList<Float>>()
    private var subject = FloatArray( model.embeddingDim )

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String,FloatArray>>()


    // <-------------- User controls --------------------------->

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "l2"


    // <-------------------------------------------------------->




    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // If the previous frame is still being processed, then skip this frame
        if ( isProcessing || faceList.size == 0 ) {
            image.close()
            return
        }
        else {
            isProcessing = true
            // Rotated bitmap for the FaceNet model
            val frameBitmap = BitmapUtils.imageToBitmap( image.image!! ,  image.imageInfo.rotationDegrees )

            // Configure frameHeight and frameWidth for output2overlay transformation matrix.
            if ( !boundingBoxOverlay.areDimsInit ) {
                boundingBoxOverlay.frameHeight = frameBitmap.height
                boundingBoxOverlay.frameWidth = frameBitmap.width
            }

            val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees )
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope( Dispatchers.Default ).launch {
                        runModel( faces , frameBitmap )
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }



    private suspend fun runModel( faces : List<Face> , cameraFrameBitmap : Bitmap ){
        withContext( Dispatchers.Default ) {
            val predictions = ArrayList<Prediction>()

            var sizeList = ArrayList<Int>()
            var max = 0     // max face size
            var maxIndex = -1   //max face index in the face array

            //find the biggest face
            for (face in faces) {
                try {
                    // set the middle 1/3 of screen as the detection area
                    if(face.boundingBox.centerX() >= 640 && face.boundingBox.centerX() <= 1280){
                        // Crop the frame and convert to ByteBuffer
                        val croppedBitmap = BitmapUtils.cropRectFromBitmap( cameraFrameBitmap , face.boundingBox )
                        var size = croppedBitmap.height * croppedBitmap.width
                        if(size > max){
                            max = size
                            maxIndex = faces.indexOf(face)
                        }
                        sizeList.add(size)
                    }

                }
                catch ( e : Exception ) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e( "Model" , "Exception in FrameAnalyser : ${e.message}" )
                    continue
                }
            }


            for (face in faces) {
                try {
                    // Crop the frame and convert to ByteBuffer
                    val croppedBitmap = BitmapUtils.cropRectFromBitmap( cameraFrameBitmap , face.boundingBox )
                    subject = model.getFaceEmbedding( croppedBitmap )

                    // Perform clustering ( grouping )
                    // Store the clusters in a HashMap. Here, the key would represent the 'name'
                    // of that cluster and ArrayList<Float> would represent the collection of all
                    // L2 norms/ cosine distances.
                    for ( i in 0 until faceList.size ) {
                        // If this cluster ( i.e an ArrayList with a specific key ) does not exist, initialize a new one.
                        if ( nameScoreHashmap[ faceList[ i ].first ] == null ) {
                            // Compute the L2 norm and then append it to the ArrayList.
                            val p = ArrayList<Float>()
                            if ( metricToBeUsed == "cosine" ) {
                                p.add( cosineSimilarity( subject , faceList[ i ].second ) )
                            }
                            else {
                                p.add( L2Norm( subject , faceList[ i ].second ) )
                            }
                            nameScoreHashmap[ faceList[ i ].first ] = p
                        }
                        // If this cluster exists, append the L2 norm/cosine score to it.
                        else {
                            if ( metricToBeUsed == "cosine" ) {
                                nameScoreHashmap[ faceList[ i ].first ]?.add( cosineSimilarity( subject , faceList[ i ].second ) )
                            }
                            else {
                                nameScoreHashmap[ faceList[ i ].first ]?.add( L2Norm( subject , faceList[ i ].second ) )
                            }
                        }
                    }

                    // Compute the average of all scores norms for each cluster.
                    val avgScores = nameScoreHashmap.values.map{ scores -> scores.toFloatArray().average() }
//                     Logger.log( "Average score for each user : $nameScoreHashmap" )

                    val names = nameScoreHashmap.keys.toTypedArray()
                    nameScoreHashmap.clear()

                    // Calculate the minimum L2 distance from the stored average L2 norms.
                    val bestScoreUserName: String = if ( metricToBeUsed == "cosine" ) {
                        // In case of cosine similarity, choose the highest value.
                        if ( avgScores.maxOrNull()!! > model.model.cosineThreshold ) {
                            names[ avgScores.indexOf( avgScores.maxOrNull()!! ) ]
                        }
                        else {
                            ""
                        }
                    } else {
                        // In case of L2 norm, choose the lowest value.
                        if ( avgScores.minOrNull()!! > model.model.l2Threshold ) {
                            ""
                        }
                        else {
                            names[ avgScores.indexOf( avgScores.minOrNull()!! ) ]
                        }
                    }

                    if(maxIndex != -1 && maxIndex == faces.indexOf(face)){
                        predictions.add(
                            Prediction(
                                face.boundingBox,
                                bestScoreUserName ,
                            )
                        )

                        //get biggest face detailed info from firestore
                        val db = Firebase.firestore
                        val docRef = db.collection("test").document(bestScoreUserName)
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    val gpa = document.getString("GPA")
                                    val strength = document.getString("strength")
                                    val weakness = document.getString("weakness")
                                    Logger.infoLog("${bestScoreUserName}'s GPA: $gpa\n" +
                                            "${bestScoreUserName}'s strength: $strength\n" +
                                            "${bestScoreUserName}'s weakness: $weakness",
                                        bestScoreUserName)
                                } else {
                                    Logger.log("No such document")
                                }
                            }
                            .addOnFailureListener { exception ->
                                Logger.log("get failed with $exception")
                            }

                    }
                    //enable multi face recognition (abandoned since poor display effect on the glasses)
//                    else{
//                        predictions.add(
//                            Prediction(
//                                face.boundingBox,
//                                bestScoreUserName ,
//                            )
//                        )
//                    }




                }
                catch ( e : Exception ) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e( "Model" , "Exception in FrameAnalyser : ${e.message}" )
                    continue
                }
            }

            withContext( Dispatchers.Main ) {
                // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
                boundingBoxOverlay.faceBoundingBoxes = predictions
                boundingBoxOverlay.invalidate()

                isProcessing = false
            }
        }
    }


    // Compute the L2 norm of ( x2 - x1 )
    private fun L2Norm( x1 : FloatArray, x2 : FloatArray ) : Float {
        return sqrt( x1.mapIndexed{ i , xi -> (xi - x2[ i ]).pow( 2 ) }.sum() )
    }


    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity( x1 : FloatArray , x2 : FloatArray ) : Float {
        val mag1 = sqrt( x1.map { it * it }.sum() )
        val mag2 = sqrt( x2.map { it * it }.sum() )
        val dot = x1.mapIndexed{ i , xi -> xi * x2[ i ] }.sum()
        return dot / (mag1 * mag2)
    }

}
