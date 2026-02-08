package com.example.pim

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pim.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var vistas: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vistas = ActivityResultBinding.inflate(layoutInflater)
        setContentView(vistas.root)

        val ganador = intent.getStringExtra("WINNER") ?: "Jugador 1"
        val colorEquipo = intent.getIntExtra("COLOR", Color.BLUE)

        vistas.txtVictory.text = "VICTORIA DE ${ganador.uppercase()}"
        vistas.txtVictory.setTextColor(colorEquipo)

        configurarBaile(colorEquipo)

        vistas.btnBackToMenu.setOnClickListener {
            val intento = Intent(this, MainActivity::class.java)
            intento.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intento)
        }
    }

    private fun configurarBaile(color: Int) {
        val iconos = listOf(
            android.R.drawable.ic_menu_help,
            android.R.drawable.ic_menu_search,
            android.R.drawable.ic_menu_manage,
            android.R.drawable.ic_menu_send
        )

        iconos.forEachIndexed { indice, recursoIcono ->
            val bailarin = ImageView(this).apply {
                setImageResource(recursoIcono)
                setColorFilter(color)
                layoutParams = LinearLayout.LayoutParams(150, 150).apply { marginEnd = 20 }
            }
            vistas.danceFloor.addView(bailarin)
            iniciarAnimacionBaile(bailarin, indice)
        }
    }

    private fun iniciarAnimacionBaile(vista: ImageView, desplazamiento: Int) {
        val salto = TranslateAnimation(0f, 0f, 0f, -50f).apply {
            duration = 400
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            startOffset = (desplazamiento * 100).toLong()
        }
        vista.startAnimation(salto)
    }
}