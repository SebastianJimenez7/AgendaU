package com.agendau.app.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.agendau.app.data.Tarea
import com.agendau.app.util.Fechas
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class InicioFragment : Fragment(R.layout.fragment_inicio) {

    private var diaElegido: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<FloatingActionButton>(R.id.btnNueva).setOnClickListener {
            mostrarFormularioTarea(requireContext()) { actualizar(view) }
        }
        actualizar(view)
    }

    private fun actualizar(view: View) {
        val pendientes = AgendaDb.obtener(requireContext()).listarTareas().filter { !it.hecha }

        view.findViewById<TextView>(R.id.txtResumen).text = when (pendientes.size) {
            0 -> "No tienes tareas pendientes"
            1 -> "Tienes 1 tarea pendiente"
            else -> "Tienes ${pendientes.size} tareas pendientes"
        }

        dibujarDias(view, pendientes)

        val dia = diaElegido
        val lista = if (dia == null) pendientes else pendientes.filter { it.fecha == dia }
        view.findViewById<TextView>(R.id.txtTituloLista).text =
            if (dia == null) "Pendientes" else "Pendientes del ${Fechas.legible(dia)}"

        val vacio = if (dia == null) {
            "No hay tareas pendientes. Pulsa + para agregar una."
        } else {
            "No hay tareas pendientes para este día."
        }
        mostrarTareas(view.findViewById(R.id.listaPendientes), lista, vacio) { actualizar(view) }
    }

    private fun dibujarDias(view: View, pendientes: List<Tarea>) {
        val franja = view.findViewById<LinearLayout>(R.id.franjaDias)
        franja.removeAllViews()
        val letras = arrayOf("D", "L", "M", "X", "J", "V", "S")
        val densidad = resources.displayMetrics.density

        for (i in 0..4) {
            val calendario = Calendar.getInstance()
            calendario.add(Calendar.DAY_OF_MONTH, i)
            val fecha = Fechas.aTexto(calendario)
            val tieneTareas = pendientes.any { it.fecha == fecha }
            val elegido = diaElegido == fecha

            val nombreDia = if (i == 0) "Hoy" else letras[calendario.get(Calendar.DAY_OF_WEEK) - 1]
            val marca = if (tieneTareas) "•" else " "

            val celda = TextView(requireContext())
            celda.text = "$nombreDia\n${calendario.get(Calendar.DAY_OF_MONTH)}\n$marca"
            celda.gravity = Gravity.CENTER
            celda.textSize = 16f
            celda.setBackgroundResource(if (elegido) R.drawable.bg_dia_sel else R.drawable.bg_dia)
            celda.setTextColor(
                ContextCompat.getColor(requireContext(), if (elegido) R.color.white else R.color.navy)
            )
            val params = LinearLayout.LayoutParams(0, (84 * densidad).toInt(), 1f)
            params.setMargins(4, 0, 4, 0)
            celda.layoutParams = params
            celda.setOnClickListener {
                diaElegido = if (elegido) null else fecha
                actualizar(view)
            }
            franja.addView(celda)
        }
    }
}
