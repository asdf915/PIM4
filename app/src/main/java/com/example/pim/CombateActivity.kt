package com.example.pim

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pim.databinding.ActivityBattleBinding
import java.util.LinkedList
import java.util.Queue
import kotlin.math.abs

class BattleActivity : AppCompatActivity() {

    private lateinit var vistas: ActivityBattleBinding
    private val filas = 8
    private val columnas = 6
    private var esTurnoJugador = true
    private var dificultad = 1
    private var recursoMapaSeleccionado: Int = 0
    private var contraIA: Boolean = true

    private val bosqueMapa1 = setOf(10, 14, 19, 22, 23, 25, 40, 42, 44)
    private val bosqueMapa2 = setOf(14, 29, 42, 44)
    private val bosqueMapa3 = setOf(5, 28, 31, 47)
    private val aguaMapa2 = setOf(3, 4, 5, 9, 10, 11, 12, 14, 15, 27, 28, 34)
    private val aguaMapa3 = setOf(12, 13, 19, 20, 22, 23)
    private val montanaMapa3 = setOf(1, 11, 37)

    private var casillasAlcanzables = mutableSetOf<Int>()
    private val mapaUnidades = mutableMapOf<Int, InstanciaUnidad>()
    private var indiceCasillaSeleccionada: Int? = null
    private var modo = ModoInteraccion.NINGUNO

    enum class ModoInteraccion { NINGUNO, MOVIMIENTO, ATAQUE, ACCION }

    enum class TipoUnidad(val nombreAMostrar: String, val vidaMax: Int, val ataque: Int, val rangoMovimiento: Int, val rangoAtaque: Int, val accionSecundaria: String, val recursoIcono: Int) {
        INFANTERIA("Infantería", 100, 30, 3, 1, "Potenciar", android.R.drawable.ic_menu_help),
        BAZOOKA("Bazooka", 80, 50, 2, 2, "Re-mover", android.R.drawable.ic_menu_search),
        TANQUE("Tanque", 180, 35, 2, 1, "Empujar", android.R.drawable.ic_menu_manage),
        AVION("Avión", 90, 25, 4, 1, "Curar", android.R.drawable.ic_menu_send)
    }

    data class InstanciaUnidad(val tipo: TipoUnidad, var vidaActual: Int, val colorEquipo: Int, var haMovido: Boolean = false, var haAtacado: Boolean = false, var haTerminadoTurno: Boolean = false, var estaPotenciada: Boolean = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vistas = ActivityBattleBinding.inflate(layoutInflater)
        setContentView(vistas.root)

        val nombreMapa = intent.getStringExtra("MAP_NAME") ?: "Mapa 1"
        contraIA = intent.getBooleanExtra("VS_AI", true)
        recursoMapaSeleccionado = when(nombreMapa) { "Mapa 2" -> R.drawable.mapa2; "Mapa 3" -> R.drawable.mapa3; else -> R.drawable.mapa1 }
        vistas.imgBattleBackground.setImageResource(recursoMapaSeleccionado)
        dificultad = getSharedPreferences("PIM_SETTINGS", Context.MODE_PRIVATE).getInt("DIFFICULTY", 1)

        ViewCompat.setOnApplyWindowInsetsListener(vistas.battleRoot) { v, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        configurarTablero()
        colocarUnidadesIniciales()

        vistas.btnMove.setOnClickListener { iniciarModoInteraccion(ModoInteraccion.MOVIMIENTO) }
        vistas.btnAttack.setOnClickListener { iniciarModoInteraccion(ModoInteraccion.ATAQUE) }
        vistas.btnAction.setOnClickListener { iniciarModoInteraccion(ModoInteraccion.ACCION) }
        vistas.btnWait.setOnClickListener { accionEsperar() }
        vistas.btnEndTurn.setOnClickListener { cambiarTurno() }
        actualizarVisibilidadBotones(null)
    }

    private fun configurarTablero() {
        val tamanoCasilla = (resources.displayMetrics.widthPixels - 60) / columnas
        vistas.mapContainer.layoutParams = vistas.mapContainer.layoutParams.apply { width = tamanoCasilla * columnas; height = tamanoCasilla * filas }

        for (i in 0 until filas * columnas) {
            val casilla = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply { width = tamanoCasilla; height = tamanoCasilla; rowSpec = GridLayout.spec(i / columnas); columnSpec = GridLayout.spec(i % columnas) }
                setOnClickListener { onCellClicked(i) }
            }
            vistas.battleGrid.addView(casilla)
        }
    }

    private fun colocarUnidadesIniciales() {
        val tipos = TipoUnidad.values()
        when (recursoMapaSeleccionado) {
            R.drawable.mapa2 -> { agregarUnidad(0, Color.RED, tipos[0]); agregarUnidad(1, Color.RED, tipos[1]); agregarUnidad(6, Color.RED, tipos[2]); agregarUnidad(7, Color.RED, tipos[3]) }
            R.drawable.mapa3 -> { agregarUnidad(3, Color.RED, tipos[0]); agregarUnidad(4, Color.RED, tipos[1]); agregarUnidad(9, Color.RED, tipos[2]); agregarUnidad(10, Color.RED, tipos[3]) }
            else -> { for (i in 1..4) agregarUnidad(i, Color.RED, tipos[i-1]) }
        }
        for (i in 1..4) agregarUnidad((filas - 1) * columnas + i, Color.BLUE, tipos[i-1])
    }

    private fun agregarUnidad(indice: Int, color: Int, tipo: TipoUnidad) {
        mapaUnidades[indice] = InstanciaUnidad(tipo, tipo.vidaMax, color)
        actualizarIUUnidad(indice)
    }

    private fun actualizarIUUnidad(indice: Int) {
        val casilla = vistas.battleGrid.getChildAt(indice) as FrameLayout
        casilla.removeAllViews()
        mapaUnidades[indice]?.let { unidad ->
            casilla.addView(ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1).apply { gravity = Gravity.CENTER; setMargins(8, 8, 8, 8) }
                setImageResource(unidad.tipo.recursoIcono)
                setColorFilter(if (unidad.estaPotenciada) Color.MAGENTA else if (unidad.haTerminadoTurno) Color.GRAY else unidad.colorEquipo)
            })
        }
    }

    private fun onCellClicked(indice: Int) {
        val objetivo = mapaUnidades[indice]
        val indiceSeleccionado = indiceCasillaSeleccionada

        when (modo) {
            ModoInteraccion.MOVIMIENTO -> {
                if (indiceSeleccionado != null && casillasAlcanzables.contains(indice) && objetivo == null) moverUnidad(indiceSeleccionado, indice)
                else Toast.makeText(this, "Inválido", Toast.LENGTH_SHORT).show()
                limpiarResaltados(); modo = ModoInteraccion.NINGUNO; verificarTodasUnidadesTerminadas()
            }
            ModoInteraccion.ATAQUE -> {
                val atacante = indiceSeleccionado?.let { mapaUnidades[it] }
                if (indiceSeleccionado != null && atacante != null && objetivo != null && objetivo.colorEquipo != atacante.colorEquipo && obtenerDistancia(indiceSeleccionado, indice) <= atacante.tipo.rangoAtaque) realizarAtaque(indiceSeleccionado, indice)
                else Toast.makeText(this, "Fuera de rango", Toast.LENGTH_SHORT).show()
                limpiarResaltados(); modo = ModoInteraccion.NINGUNO; verificarTodasUnidadesTerminadas()
            }
            ModoInteraccion.ACCION -> {
                if (indiceSeleccionado != null && obtenerDistancia(indiceSeleccionado, indice) == 1) realizarAccionSecundaria(indiceSeleccionado, indice)
                else Toast.makeText(this, "Fuera de rango", Toast.LENGTH_SHORT).show()
                limpiarResaltados(); modo = ModoInteraccion.NINGUNO; verificarTodasUnidadesTerminadas()
            }
            else -> {
                limpiarResaltados(); indiceCasillaSeleccionada = indice; resaltarCasilla(indice, Color.argb(120, 255, 255, 0))
                if (objetivo != null) {
                    val equipo = if (objetivo.colorEquipo == Color.BLUE) "P1" else if (contraIA) "IA" else "P2"
                    vistas.txtInfo.text = "${objetivo.tipo.nombreAMostrar} ($equipo)\nVida: ${objetivo.vidaActual}/${objetivo.tipo.vidaMax} | ATK: ${objetivo.tipo.ataque}${if (objetivo.estaPotenciada) " +10" else ""}"
                    vistas.btnAction.text = objetivo.tipo.accionSecundaria; actualizarVisibilidadBotones(objetivo)
                } else {
                    vistas.txtInfo.text = "Casilla: [${indice / columnas}, ${indice % columnas}]"; actualizarVisibilidadBotones(null)
                }
            }
        }
    }

    private fun iniciarModoInteraccion(m: ModoInteraccion) {
        val u = indiceCasillaSeleccionada?.let { mapaUnidades[it] } ?: return
        if (u.haTerminadoTurno || !esEquipoActual(u)) return
        modo = m
        val indice = indiceCasillaSeleccionada!!
        when (m) {
            ModoInteraccion.MOVIMIENTO -> {
                calcularCasillasAlcanzables(indice, u.tipo.rangoMovimiento)
                casillasAlcanzables.forEach { if (it != indice && mapaUnidades[it] == null) resaltarCasilla(it, Color.argb(120, 0, 0, 255)) }
            }
            ModoInteraccion.ATAQUE -> resaltarRango(indice, u.tipo.rangoAtaque, Color.argb(120, 255, 0, 0))
            ModoInteraccion.ACCION -> resaltarRango(indice, 1, Color.argb(120, 255, 0, 255))
            else -> {}
        }
    }

    private fun moverUnidad(desde: Int, hasta: Int) {
        val u = mapaUnidades.remove(desde) ?: return
        u.haMovido = true; mapaUnidades[hasta] = u; indiceCasillaSeleccionada = hasta
        actualizarIUUnidad(desde); actualizarIUUnidad(hasta); actualizarVisibilidadBotones(u); resaltarCasilla(hasta, Color.argb(120, 255, 255, 0))
    }

    private fun realizarAtaque(indiceAtacante: Int, indiceObjetivo: Int) {
        val atacante = mapaUnidades[indiceAtacante] ?: return
        val objetivo = mapaUnidades[indiceObjetivo] ?: return
        var daño = atacante.tipo.ataque + (if (atacante.estaPotenciada) 10 else 0)
        if (esBosque(indiceObjetivo)) daño = (daño * 0.7).toInt()
        objetivo.vidaActual -= daño; atacante.haAtacado = true; atacante.haTerminadoTurno = true
        if (objetivo.vidaActual <= 0) mapaUnidades.remove(indiceObjetivo)
        actualizarIUUnidad(indiceAtacante); actualizarIUUnidad(indiceObjetivo); verificarFinJuego()
    }

    private fun realizarAccionSecundaria(indiceActor: Int, indiceObjetivo: Int) {
        val actor = mapaUnidades[indiceActor] ?: return
        val objetivo = mapaUnidades[indiceObjetivo] ?: return
        when (actor.tipo) {
            TipoUnidad.INFANTERIA -> if (objetivo.colorEquipo == actor.colorEquipo) objetivo.estaPotenciada = true
            TipoUnidad.BAZOOKA -> if (objetivo.colorEquipo == actor.colorEquipo) { objetivo.haMovido = false; objetivo.haAtacado = false; objetivo.haTerminadoTurno = false }
            TipoUnidad.TANQUE -> {
                val filaN = (indiceObjetivo / columnas) * 2 - (indiceActor / columnas); val colN = (indiceObjetivo % columnas) * 2 - (indiceActor % columnas)
                if (filaN in 0 until filas && colN in 0 until columnas && mapaUnidades[filaN * columnas + colN] == null) {
                    mapaUnidades.remove(indiceObjetivo); mapaUnidades[filaN * columnas + colN] = objetivo; actualizarIUUnidad(indiceObjetivo); actualizarIUUnidad(filaN * columnas + colN)
                }
            }
            TipoUnidad.AVION -> if (objetivo.colorEquipo == actor.colorEquipo) objetivo.vidaActual = (objetivo.vidaActual + 40).coerceAtMost(objetivo.tipo.vidaMax)
        }
        actor.haAtacado = true; actor.haTerminadoTurno = true; actualizarIUUnidad(indiceActor); actualizarIUUnidad(indiceObjetivo)
    }

    private fun accionEsperar() {
        indiceCasillaSeleccionada?.let { indice ->
            mapaUnidades[indice]?.let { it.haTerminadoTurno = true; actualizarIUUnidad(indice); actualizarVisibilidadBotones(it); verificarTodasUnidadesTerminadas() }
        }
    }

    private fun cambiarTurno() {
        esTurnoJugador = !esTurnoJugador
        mapaUnidades.values.forEach { if (esEquipoActual(it)) { it.haMovido = false; it.haAtacado = false; it.haTerminadoTurno = false; it.estaPotenciada = false } }
        for (i in 0 until filas * columnas) actualizarIUUnidad(i)
        vistas.txtTurnPhase.text = if (esTurnoJugador) "Turno Jugador 1" else if (contraIA) "Turno IA" else "Turno Jugador 2"
        vistas.txtTurnPhase.setTextColor(if (esTurnoJugador) Color.parseColor("#FFD700") else Color.RED)
        limpiarResaltados(); indiceCasillaSeleccionada = null; actualizarVisibilidadBotones(null)
        if (!esTurnoJugador && contraIA) Handler(Looper.getMainLooper()).postDelayed({ ejecutarIAEnemiga() }, 1000)
    }

    private fun verificarTodasUnidadesTerminadas() {
        val color = if (esTurnoJugador) Color.BLUE else Color.RED
        if (mapaUnidades.values.filter { it.colorEquipo == color }.all { it.haTerminadoTurno }) Handler(Looper.getMainLooper()).postDelayed({ cambiarTurno() }, 500)
    }

    private fun ejecutarIAEnemiga() {
        val entradaEnemigo = mapaUnidades.filter { it.value.colorEquipo == Color.RED && !it.value.haTerminadoTurno }.entries.firstOrNull()
            ?: run { cambiarTurno(); return }

        val indiceEnemigo = entradaEnemigo.key
        val unidadEnemiga = entradaEnemigo.value
        val objetivos = mapaUnidades.filter { it.value.colorEquipo == Color.BLUE }

        if (objetivos.isNotEmpty()) {
            val masCercano = objetivos.minByOrNull { obtenerDistancia(indiceEnemigo, it.key) }!!
            if (obtenerDistancia(indiceEnemigo, masCercano.key) <= unidadEnemiga.tipo.rangoAtaque) {
                realizarAtaque(indiceEnemigo, masCercano.key)
            } else {
                calcularCasillasAlcanzables(indiceEnemigo, unidadEnemiga.tipo.rangoMovimiento)
                val mejorMovimiento = casillasAlcanzables.filter { mapaUnidades[it] == null }.minByOrNull { obtenerDistancia(it, masCercano.key) } ?: indiceEnemigo
                if (mejorMovimiento != indiceEnemigo) moverUnidad(indiceEnemigo, mejorMovimiento)
                val objetivoTrasMovimiento = mapaUnidades.filter { it.value.colorEquipo == Color.BLUE && obtenerDistancia(mejorMovimiento, it.key) <= unidadEnemiga.tipo.rangoAtaque }.keys.firstOrNull()
                if (objetivoTrasMovimiento != null) realizarAtaque(mejorMovimiento, objetivoTrasMovimiento)
            }
        }
        unidadEnemiga.haTerminadoTurno = true
        actualizarIUUnidad(indiceEnemigo)
        Handler(Looper.getMainLooper()).postDelayed({ ejecutarIAEnemiga() }, 800)
    }

    private fun calcularCasillasAlcanzables(inicio: Int, rango: Int) {
        casillasAlcanzables.clear(); val cola: Queue<Pair<Int, Int>> = LinkedList(); cola.add(inicio to 0); casillasAlcanzables.add(inicio)
        val unidad = mapaUnidades[inicio] ?: return
        while (cola.isNotEmpty()) {
            val (actual, dist) = cola.poll()!!
            if (dist < rango) {
                val r = actual / columnas; val c = actual % columnas
                listOf(r-1 to c, r+1 to c, r to c-1, r to c+1).forEach { (nr, nc) ->
                    val nIndice = nr * columnas + nc
                    if (nr in 0 until filas && nc in 0 until columnas && mapaUnidades[nIndice] == null && puedeEntrar(nIndice, unidad) && casillasAlcanzables.add(nIndice)) cola.add(nIndice to dist + 1)
                }
            }
        }
    }

    private fun puedeEntrar(i: Int, u: InstanciaUnidad) = !esMontana(i) && (u.tipo == TipoUnidad.AVION || !esAgua(i))
    private fun esBosque(i: Int) = when(recursoMapaSeleccionado) { R.drawable.mapa1 -> bosqueMapa1.contains(i); R.drawable.mapa2 -> bosqueMapa2.contains(i); else -> bosqueMapa3.contains(i) }
    private fun esAgua(i: Int) = (recursoMapaSeleccionado == R.drawable.mapa2 && aguaMapa2.contains(i)) || (recursoMapaSeleccionado == R.drawable.mapa3 && aguaMapa3.contains(i))
    private fun esMontana(i: Int) = recursoMapaSeleccionado == R.drawable.mapa3 && montanaMapa3.contains(i)
    private fun obtenerDistancia(i1: Int, i2: Int) = abs(i1 / columnas - i2 / columnas) + abs(i1 % columnas - i2 % columnas)
    private fun esEquipoActual(u: InstanciaUnidad) = if (esTurnoJugador) u.colorEquipo == Color.BLUE else u.colorEquipo == Color.RED
    private fun resaltarCasilla(i: Int, c: Int) { (vistas.battleGrid.getChildAt(i) as FrameLayout).setBackgroundColor(c) }
    private fun limpiarResaltados() { for (i in 0 until filas * columnas) resaltarCasilla(i, Color.TRANSPARENT) }
    private fun resaltarRango(centro: Int, rango: Int, color: Int) { for (i in 0 until filas * columnas) if (i != centro && obtenerDistancia(centro, i) <= rango) resaltarCasilla(i, color) }

    private fun actualizarVisibilidadBotones(u: InstanciaUnidad?) {
        val habilitado = u != null && !u.haTerminadoTurno && esEquipoActual(u)
        vistas.btnMove.isEnabled = habilitado && !u!!.haMovido; vistas.btnAttack.isEnabled = habilitado && !u!!.haAtacado
        vistas.btnAction.isEnabled = habilitado && !u!!.haAtacado; vistas.btnWait.isEnabled = habilitado
    }

    private fun verificarFinJuego() {
        val azul = mapaUnidades.values.any { it.colorEquipo == Color.BLUE }; val rojo = mapaUnidades.values.any { it.colorEquipo == Color.RED }
        if (!rojo) terminarJuego("Jugador 1", Color.BLUE) else if (!azul) terminarJuego(if (contraIA) "IA" else "Jugador 2", Color.RED)
    }

    private fun terminarJuego(ganador: String, color: Int) {
        startActivity(Intent(this, ResultActivity::class.java).apply { putExtra("WINNER", ganador); putExtra("COLOR", color) }); finish()
    }
}