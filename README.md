Smart Glass AR project

Introduction:
This project is using MLkit and FaceNet model to do real-time face recognition on Google Glass 2.

How we realize:
1. When the application starts, it will fetch stored face images from firebase and convert them to 128-dimensional embedding.
2. Setting up a preview and imageAnalyser using CameraX.
3. Using MLkit to do face detection and crop them from captured video stream frames.
4. Converting cropped face images from a bitmap to ByteBuffer and feeding the it to the FaceNet model.
5. Comparing stored embedding with captured face then pick result with the best score.
  
Other features:
1. Considering the convenience, there are 2 ways users can fetch face data. One is downloading from firebase storage. The other way is that after the data gets downloaded once, it will be automatically converted to embedding data and stored in the application's internal storage, so users only need to load this stored data instead of waiting for downloading.
2. Our application can not only recognize faces but also display the personal information of the recognized person. To realize this function, once the person gets recognized, the application will query data from the firebase database using the name, and display this information on the screen.
3. 
