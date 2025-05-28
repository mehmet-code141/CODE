package com.example.konet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private lateinit var binding: ActivityMainBinding
private lateinit var auth : FirebaseAuth
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = Firebase.auth
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
      /*  val kullanıcı= auth.currentUser
        if(kullanıcı!=null){
            var intenti= Intent(this,HomepageActivity::class.java)
            startActivity(intenti)
            finish()

        }*/
    }
    fun girisi(View: View){
        var email= binding.email.text.toString()
        var sifre= binding.password.text.toString()

        if(email.equals("")||sifre.equals("")){
            Toast.makeText(this, "lütfen boş bırakılan yerlere email veya şifrenizi giriniz", Toast.LENGTH_SHORT).show()
        }
        else(
                auth.signInWithEmailAndPassword(email,sifre).addOnSuccessListener {
                    Toast.makeText(this, "giriş işlemi başarılı", Toast.LENGTH_SHORT).show()
                    var intent= Intent(this,HomePageActivity::class.java)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "giriş işlemi başarısız", Toast.LENGTH_SHORT).show()
                }
                )
    }
    fun kaydol(View: View){
        var intent= Intent(this,RegisterActivity::class.java)
        startActivity(intent)

    }


}