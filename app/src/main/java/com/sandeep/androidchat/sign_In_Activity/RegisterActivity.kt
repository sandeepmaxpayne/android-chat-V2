package com.sandeep.androidchat.sign_In_Activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.sandeep.androidchat.chat_Message.MessageActivity
import com.sandeep.androidchat.R
import com.sandeep.androidchat.model.User
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_register)

        register_button.setOnClickListener {

                performRegister()

        }
        already_have_Account.setOnClickListener {
            Log.d("RegisterActivity", "Try to show log in activity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
       selectPhoto_Register.setOnClickListener {
           Log.d("Main", "Display the Photo")
           val intent = Intent(Intent.ACTION_PICK)
           intent.type = "image/*"
           startActivityForResult(intent, 0)
       }
    }
    var selectedPhotoURI: Uri? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            //proceed with the image check
            Log.d("Register" ,"Photo was Selected")

            selectedPhotoURI = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoURI)
            selectPhotoImageView.setImageBitmap(bitmap)
            selectPhoto_Register.alpha = 0f

//            val bitmapDrawable = BitmapDrawable(bitmap)
//            selectPhoto_Register.setBackgroundDrawable(bitmapDrawable)
        }
    }

    private fun performRegister(){
        val selectProfilePhoto = selectedPhotoURI
        val username = usernameID.text.toString()
        val email = emailID.text.toString()
        val password = passwordID.text.toString()
        if (selectProfilePhoto == null){
            Toast.makeText(this, "Please upload Profile Photo", Toast.LENGTH_SHORT).show()

        }
        if (username.isEmpty()){
            Toast.makeText(this, "Username should not be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(this , "Please enter the Email and Password", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("RegisterActivity", "Email :$email")
        Log.d("RegisterActivity", "Password : $password")
        //FireBase Authentication for user login
//        FirebaseApp.initializeApp(this)


        val userList = mutableListOf<String>()
        FirebaseDatabase.getInstance().reference.child("users").apply {
            addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val children = snapshot.children

                    children.forEach {
                        val userName = it.child("userName").value
                        userList.add(userName.toString())
                    }


                    val result = username in userList
                    if (result){
                        Toast.makeText(this@RegisterActivity, "Username already taken ! Try different username", Toast.LENGTH_SHORT).show()
                        return
                    }else{
                        //load user
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener {
                                if (!it.isSuccessful) {return@addOnCompleteListener}
                                else {
                                    Toast.makeText(this@RegisterActivity, "successfully registered", Toast.LENGTH_LONG).show()
                                    verifyEmail()
                                    uploadImageFireBaseStorage()
                                    Log.d("Main", "Successfully registered ${it.result?.user?.uid}")


                                }
                            }
                            .addOnFailureListener{
                                Toast.makeText(this@RegisterActivity, "failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
                                Log.d("Main", "${it.message}")

                            }


                    }


                }
                override fun onCancelled(error: DatabaseError) {
                    Log.d("error", error.message)
                }

            }


            )
        }



    }
    private fun uploadImageFireBaseStorage(){
        if(selectedPhotoURI == null){ return }
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoURI!!)
            .addOnSuccessListener { it ->
                Log.d("Register", "successfully uploaded image: ${it.metadata?.path}")
                ref.downloadUrl.addOnSuccessListener {
                    it.toString()
                    Log.d("Register" ,"file location: $it")

                    saveUserToFiredaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                Log.d("Register", "${it.message}")
            }
        }
    private fun saveUserToFiredaseDatabase(profileImageUrl: String){
        val uid = FirebaseAuth.getInstance().uid?: ""
       val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, usernameID.text.toString(),profileImageUrl)

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("Register", "Finally user saved to Firebase Database")
                val intent = Intent(this, MessageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener{
                Log.d("Register", "Error on save user to database ${it.message}")
            }
    }
    private fun verifyEmail() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) {
                if(it.isSuccessful){
                    val intent = Intent(this, MessageActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    Toast.makeText(this@RegisterActivity, "Email Verification Sent on ${user.email}", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this@RegisterActivity, "Failed to send Email Verification", Toast.LENGTH_SHORT).show()
                }
            }
    }


}

//@Parcelize
//class User(val uid: String, val userName: String, val profileImageUrl: String):Parcelable{
//    constructor(): this("","", "")
//}

