package com.example.konet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.konet.databinding.ActivityHomepageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class HomePageActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomepageBinding
    private lateinit var tts: TextToSpeech

    private val mesajlar = mutableListOf<Mesaj>()
    private lateinit var mesajAdapter: MesajAdapter

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private val RECORD_AUDIO_REQUEST_CODE = 42

    private val apiAnahtari = "AIzaSyBvfG--mgjnPx5auE1MYfz0-8-P96tN2Pc"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        tts = TextToSpeech(this, this)

        mesajAdapter = MesajAdapter(mesajlar, tts)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mesajAdapter

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            setupSpeech()
        }

        binding.butonMikrofon.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
                return@setOnClickListener
            }
            speechRecognizer.startListening(speechIntent)
            Toast.makeText(this, "Konuşmaya başlayabilirsiniz...", Toast.LENGTH_SHORT).show()
        }

        binding.butonGonder.setOnClickListener {
            val girdi = binding.editMesaj.text.toString().trim()
            if (girdi.isNotEmpty()) {
                mesajlar.add(Mesaj(girdi, true))
                mesajlar.add(Mesaj("Yanıt bekleniyor...", false))
                mesajAdapter.notifyDataSetChanged()
                binding.recyclerView.scrollToPosition(mesajlar.size - 1)
                binding.editMesaj.setText("")

                geminiRestIleGonder(girdi)
            }
        }
    }

    private fun setupSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@HomePageActivity, "Ses algılanamadı, tekrar deneyin.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    if (it.isNotEmpty()) {
                        binding.editMesaj.setText(it[0])
                        binding.editMesaj.setSelection(binding.editMesaj.text.length)
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupSpeech()
        } else {
            Toast.makeText(this, "Sesli giriş için mikrofon izni gerekli.", Toast.LENGTH_SHORT).show()
        }
    }

    fun cikis(view: View) {
        auth.signOut()
        val intent = Intent(this, selectActivity::class.java)
        startActivity(intent)
    }

    // Doğrudan soruya insani ve alakalı tek cevap verir
    private fun geminiRestIleGonder(girdi: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiAnahtari"

            // Kullanıcının sorusuna DOĞRUDAN ve insani tek bir cevap için prompt
            val prompt = """
Kullanıcı şunu sordu: "$girdi"
Lütfen bu soruya, bir insan gibi, doğal ve samimi bir sohbet diliyle ve DOĞRUDAN bu soruya uygun, anlamlı ve mantıklı tek bir cevap ver.
Bu soruya verilebilecek Her cevap farklı bir insanın bakış açısından, biri olumlu biri olumsuz ruh haliyle yazılmış olsun. Cevaplar birbirini desteklemesin.
Cevabın, kullanıcının sorduğu soruya gerçekten karşılık versin.
Cevapları sadece aşağıdaki formatta sırala:
" "(ilk cevap)
" "(ikinci cevap)
""".trimIndent()

            val icerikNesne = JSONObject()
            icerikNesne.put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            val anaNesne = JSONObject()
            anaNesne.put("contents", JSONArray().put(icerikNesne))

            val istemci = OkHttpClient()
            val govde = anaNesne.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val istek = Request.Builder()
                .url(url)
                .post(govde)
                .build()

            istemci.newCall(istek).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        if (mesajlar.isNotEmpty() && mesajlar.last().icerik == "Yanıt bekleniyor...") {
                            mesajlar.removeAt(mesajlar.size - 1)
                        }
                        mesajlar.add(Mesaj("Bağlantı hatası: ${e.localizedMessage}", false))
                        mesajAdapter.notifyDataSetChanged()
                        binding.recyclerView.scrollToPosition(mesajlar.size - 1)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val yanitGovde = response.body?.string()
                    runOnUiThread {
                        if (mesajlar.isNotEmpty() && mesajlar.last().icerik == "Yanıt bekleniyor...") {
                            mesajlar.removeAt(mesajlar.size - 1)
                        }
                        try {
                            if (response.isSuccessful && yanitGovde != null) {
                                val obj = JSONObject(yanitGovde)
                                val candidates = obj.optJSONArray("candidates")
                                if (candidates != null && candidates.length() > 0) {
                                    val cevap = candidates.getJSONObject(0)
                                        .optJSONObject("content")
                                        ?.optJSONArray("parts")
                                        ?.optJSONObject(0)
                                        ?.optString("text") ?: ""
                                    mesajlar.add(Mesaj(cevap.trim(), false))
                                } else {
                                    mesajlar.add(Mesaj("API beklenmedik yanıt döndü.", false))
                                }
                            } else {
                                mesajlar.add(Mesaj("API hatası: ${response.message}", false))
                            }
                        } catch (e: Exception) {
                            mesajlar.add(Mesaj("Yanıt işlenemedi: ${e.localizedMessage}", false))
                        }
                        mesajAdapter.notifyDataSetChanged()
                        binding.recyclerView.scrollToPosition(mesajlar.size - 1)
                    }
                }
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("tr", "TR")
        }
    }

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}