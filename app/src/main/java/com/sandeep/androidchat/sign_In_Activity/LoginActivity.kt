package com.sandeep.androidchat.sign_In_Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.sandeep.androidchat.chat_Message.MessageActivity
import com.sandeep.androidchat.R
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null){
            val startHomePage = Intent(this@LoginActivity, MessageActivity::class.java)
            startActivity(startHomePage)
            finish()
        }
        setContentView(R.layout.activity_login)
        buLogin.setOnClickListener {
            val email = emailLogin.text.toString()
            val password = passwordLogin.text.toString()
            Log.d("login", "Attempt to Login.")

            if (email.isEmpty() && password.isEmpty()){
                Toast.makeText(this@LoginActivity, "Please enter email and password !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (!it.isSuccessful){
                        return@addOnCompleteListener
                    }else{
                       // Log.d("Log", "Successfully Logged in ${it.result.user}")
                        Toast.makeText(this@LoginActivity, "Logged in Successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MessageActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }

                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    Log.d("Log", "Error signing in: ${it.message}")

                }
        }
        back_to_registration.setOnClickListener {
            val register = Intent(this@LoginActivity, RegisterActivity::class.java)
            register.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(register)
        }
        forgot_password.setOnClickListener {
            val intent = Intent(this@LoginActivity, ResetPassword::class.java)
            startActivity(intent)
        }
    }

}