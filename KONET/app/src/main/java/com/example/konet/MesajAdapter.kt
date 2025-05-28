package com.example.konet

import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.konet.databinding.ItemMesajGonderenBinding
import com.example.konet.databinding.ItemMesajAlanBinding

class MesajAdapter(
    private val mesajlar: List<Mesaj>,
    private val tts: TextToSpeech?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIP_GONDEREN = 1
        private const val TIP_ALAN = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (mesajlar[position].kullaniciGonderdi) TIP_GONDEREN else TIP_ALAN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIP_GONDEREN) {
            val binding = ItemMesajGonderenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            GonderenMesajViewHolder(binding)
        } else {
            val binding = ItemMesajAlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AlanMesajViewHolder(binding, tts)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mesaj = mesajlar[position]
        if (holder is GonderenMesajViewHolder) {
            holder.bagla(mesaj)
        } else if (holder is AlanMesajViewHolder) {
            holder.bagla(mesaj)
        }
    }

    override fun getItemCount(): Int = mesajlar.size

    class GonderenMesajViewHolder(private val binding: ItemMesajGonderenBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bagla(mesaj: Mesaj) {
            binding.textMesaj.text = mesaj.icerik
        }
    }

    class AlanMesajViewHolder(
        private val binding: ItemMesajAlanBinding,
        private val tts: TextToSpeech?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bagla(mesaj: Mesaj) {

            binding.cevapSecenekleri.visibility = View.VISIBLE


            val bolunmus: List<String> = mesaj.icerik
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .let {
                    if (it.size >= 2) it
                    else mesaj.icerik.split(".")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }

            val cevap1 = bolunmus.getOrNull(0) ?: mesaj.icerik
            val cevap2 = bolunmus.getOrNull(1) ?: cevap1

            binding.butonCevap1.text = cevap1
            binding.butonCevap2.text = cevap2

            binding.butonCevap1.setOnClickListener {
                tts?.speak(cevap1, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            binding.butonCevap2.setOnClickListener {
                tts?.speak(cevap2, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }
}