package com.example.pim

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pim.databinding.ActivityMainBinding
import com.example.pim.databinding.OpcionesDialogoBinding

class MainActivity : AppCompatActivity() {

    private lateinit var vistas: ActivityMainBinding
    private var reproductor: MediaPlayer? = null
    private var volumenMusica: Float = 0.7f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vistas = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vistas.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(vistas.main) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        configurarDesfileUnidades()

        vistas.btnOpciones.setOnClickListener { mostrarDialogoOpciones() }
        vistas.btnStart.setOnClickListener {
            startActivity(Intent(this, MapasActivity::class.java))
        }
    }

    private fun mostrarDialogoOpciones() {
        val dialogo = Dialog(this)
        val vistasDialogo = OpcionesDialogoBinding.inflate(layoutInflater)
        dialogo.setContentView(vistasDialogo.root)
        dialogo.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dificultades = arrayOf("RECLUTA (Fácil)", "SOLDADO (Normal)", "VETERANO (Difícil)")
        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, dificultades)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vistasDialogo.spinnerDificultad.adapter = adaptador

        val preferencias = getSharedPreferences("PIM_SETTINGS", Context.MODE_PRIVATE)
        vistasDialogo.spinnerDificultad.setSelection(preferencias.getInt("DIFICULTAD", 1))

        vistasDialogo.Music.progress = (volumenMusica * 100).toInt()
        vistasDialogo.Music.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progreso: Int, desdeUsuario: Boolean) {
                volumenMusica = progreso / 100f
                reproductor?.setVolume(volumenMusica, volumenMusica)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vistasDialogo.botonGuardar.setOnClickListener {
            preferencias.edit().putInt("DIFICULTAD", vistasDialogo.spinnerDificultad.selectedItemPosition).apply()
            dialogo.dismiss()
        }

        dialogo.show()
    }

    override fun onResume() {
        super.onResume()
        configurarMusica()
    }

    override fun onPause() {
        super.onPause()
        reproductor?.pause()
    }

    private fun configurarDesfileUnidades() {
        vistas.unidadesContainer.removeAllViews()

        val unidades = listOf(
            DatosUnidad(Color.RED, android.R.drawable.ic_menu_help),
            DatosUnidad(Color.RED, android.R.drawable.ic_menu_search),
            DatosUnidad(Color.RED, android.R.drawable.ic_menu_manage),
            DatosUnidad(Color.RED, android.R.drawable.ic_menu_send),
            DatosUnidad(Color.BLUE, android.R.drawable.ic_menu_help),
            DatosUnidad(Color.BLUE, android.R.drawable.ic_menu_search),
            DatosUnidad(Color.BLUE, android.R.drawable.ic_menu_manage),
            DatosUnidad(Color.BLUE, android.R.drawable.ic_menu_send)
        )

        for (unidad in unidades) {
            val imagenUnidad = ImageView(this).apply {
                setImageResource(unidad.recursoIcono)
                setColorFilter(unidad.color)
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { marginEnd = 60 }
            }
            vistas.unidadesContainer.addView(imagenUnidad)
        }

        vistas.unidadesContainer.startAnimation(TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
            Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f
        ).apply {
            duration = 12000
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        })
    }

    private data class DatosUnidad(val color: Int, val recursoIcono: Int)

    private fun configurarMusica() {
        try {
            if (reproductor == null) {
                reproductor = MediaPlayer.create(this, R.raw.musica_guerra).apply { isLooping = true }
            }
            reproductor?.setVolume(volumenMusica, volumenMusica)
            if (reproductor?.isPlaying == false) reproductor?.start()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al reproducir música", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reproductor?.release()
        reproductor = null
    }
}
