package com.example.pim

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pim.databinding.ActivityMapSelectionBinding

class MapSelectionActivity : AppCompatActivity() {
    private lateinit var vistas: ActivityMapSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vistas = ActivityMapSelectionBinding.inflate(layoutInflater)
        setContentView(vistas.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(vistas.root) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        vistas.cardMap1.setOnClickListener { mostrarSeleccionModo("Mapa 1") }
        vistas.cardMap2.setOnClickListener { mostrarSeleccionModo("Mapa 2") }
        vistas.cardMap3.setOnClickListener { mostrarSeleccionModo("Mapa 3") }
    }

    private fun mostrarSeleccionModo(nombreMapa: String) {
        val opciones = arrayOf("Contra IA", "Contra Jugador 2")
        AlertDialog.Builder(this)
            .setTitle("Selecciona Modo de Juego")
            .setItems(opciones) { _, cual -> iniciarBatalla(nombreMapa, cual == 0) }
            .show()
    }

    private fun iniciarBatalla(nombreMapa: String, esContraIA: Boolean) {
        try {
            val intento = Intent(this, BattleActivity::class.java).apply {
                putExtra("MAP_NAME", nombreMapa)
                putExtra("VS_AI", esContraIA)
            }
            startActivity(intento)
        } catch (e: Exception) {
            Log.e("MapSelection", "Error al iniciar batalla", e)
        }
    }
}