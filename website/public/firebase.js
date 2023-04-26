// Import the functions you need from the SDKs you need
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.17.1/firebase-app.js";
import { getStorage, ref,  uploadBytesResumable, getDownloadURL, listAll } from "https://www.gstatic.com/firebasejs/9.17.1/firebase-storage.js";
import { getFirestore, setDoc, doc} from "https://www.gstatic.com/firebasejs/9.17.1/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyBjW4g7Uz41ERRQvXuxiOR8pWHZ-TQWsk0",
  authDomain: "smart-glass-ar.firebaseapp.com",
  projectId: "smart-glass-ar",
  storageBucket: "smart-glass-ar.appspot.com",
  messagingSenderId: "78438703824",
  appId: "1:78438703824:web:4b63168db1389b1aff34d5",
  measurementId: "G-3JSQGFVZHZ"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const storage = getStorage(app);
const db = getFirestore(app)


var userName = [];

const image_input = document.getElementById("image_input");

var fileList;
image_input.addEventListener("change", handleFiles, false);
function handleFiles() {
  fileList = this.files; /* now you can work with the file list */
}



const upload = document.getElementById("upload");
upload.addEventListener("click", uploadFunction);




async function uploadFunction(){
    var text_input = document.getElementById("text_input").value;
    var gpa_input = document.getElementById("gpa_input").value;
    var strength_input = document.getElementById("strength_input").value;
    var weakness_input = document.getElementById("weakness_input").value;
    var result = false;
    // check for duplication
    userName.forEach(names =>{
        if(text_input == names){
            if (confirm("Duplicate name detected, is this your first time upload photos?\nYes if you are, cancel if it's not") == true) {
                alert("please choose another perfered name")
                result = true;
            }else{
                result = false;
            }
        }
    });
    if(result == false){
      //upload to firestore database
      await setDoc(doc(db, "test", text_input), {
        GPA: gpa_input,
        strength: strength_input,
        weakness: weakness_input
      });

      //upload to firebase storage
      var imageName = [];
      if(fileList.length != 0){
          for(var i = 0; i < fileList.length; i++){
            fileList[i]
            imageName[i] = text_input + "_" + (i+1).toString();
            console.log(imageName[i]);
            const singleImageRef = ref(storage, ("faces/"+text_input+"/"+imageName[i]));
            const metadata = {
                contentType: 'image/jpeg',
            };  
            const uploadTask = uploadBytesResumable(singleImageRef, fileList[i], metadata);

            uploadTask.on('state_changed', 
            (snapshot) => {
                const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
                console.log('Upload is ' + progress + '% done');
                const progressBar = document.getElementById('progressBar');
                progressBar.value = progress
            }, 
            (error) => {
                // Handle unsuccessful uploads
            }, 
            () => {
                getDownloadURL(uploadTask.snapshot.ref).then((downloadURL) => {
                console.log('File available at', downloadURL);
                alert("upload successfully");
                });
            }
            );

          }
      }
    }

}




const listRef = ref(storage, 'faces');

listAll(listRef)
.then((res) => {
    res.prefixes.forEach((folderRef) => {
        userName.push(folderRef.name);
    }
    );
}).catch((error) => {
    alert("error");
});