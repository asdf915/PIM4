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
import com.example.pim.databinding.ActivityResultadoBinding

class ResultadoActivity : AppCompatActivity() {

    private lateinit var vistas: ActivityResultadoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vistas = ActivityResultadoBinding.inflate(layoutInflater)
        setContentView(vistas.root)

        val ganador = intent.getStringExtra("GANADOR") ?: "Jugador 1"
        val colorEquipo = intent.getIntExtra("COLOR", Color.BLUE)

        vistas.txtVictoria.text = "VICTORIA DE ${ganador.uppercase()}"
        vistas.txtVictoria.setTextColor(colorEquipo)

        configurarBaile(colorEquipo)

        vistas.btnvolver.setOnClickListener {
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
            val unidad = ImageView(this).apply {
                setImageResource(recursoIcono)
                setColorFilter(color)
                layoutParams = LinearLayout.LayoutParams(150, 150).apply { marginEnd = 20 }
            }
            vistas.contenedorUnidades.addView(unidad)
            iniciarAnimacionBaile(unidad, indice)
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
