package com.example.konet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.konet.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private lateinit var binding: ActivityMainBinding
private lateinit var auth: FirebaseAuth

class MainActivity : AppCompatActivity() {

    // EyeTracking isteği var mı?
    private var eyeTrackingRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = Firebase.auth
        enableEdgeToEdge()

        // EyeTracking isteği SelectActivity'den iletildiyse al
        eyeTrackingRequested = intent.getBooleanExtra("eyeTracking", false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun girisi(View: View) {
        val email = binding.email.text.toString()
        val sifre = binding.password.text.toString()

        if (email == "" || sifre == "") {
            Toast.makeText(this, "lütfen boş bırakılan yerlere email veya şifrenizi giriniz", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(email, sifre)
                .addOnSuccessListener {
                    Toast.makeText(this, "giriş işlemi başarılı", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomePageActivity::class.java)
                    // EyeTracking isteğini HomePageActivity'ye ilet
                    intent.putExtra("eyeTracking", eyeTrackingRequested)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "giriş işlemi başarısız", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun kaydol(View: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}