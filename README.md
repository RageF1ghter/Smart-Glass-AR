Smart Glass AR project

Introduction:
    This project is using MLkit and FaceNet model to do real-time face recognition on Google Glass 2.

How we realize:
    1. When the application starts, it will fetch stored face images from firebase and convert them to 
   1. Setting up a preview and imageAnalyser using CameraX.
   2. Using MLkit to do face detection and crop them from captured video stream frames.
   3. Convert cropped face images from a bitmap to ByteBuffer
  
