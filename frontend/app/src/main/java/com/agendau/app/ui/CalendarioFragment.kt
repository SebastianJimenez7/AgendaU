package com.agendau.app.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.agendau.app.util.Fechas
import java.util.Calendar
import java.util.Locale

class CalendarioFragment : Fragment(R.layout.fragment_calendario) {

    private val mes: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var diaElegido: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btnAnterior).setOnClickListener {
            mes.add(Calendar.MONTH, -1)
            diaElegido = null
            dibujar(view)
        }
        view.findViewById<Button>(R.id.btnSiguiente).setOnClickListener {
            mes.add(Calendar.MONTH, 1)
            diaElegido = null
            dibujar(view)
        }
        dibujar(view)
    }

    private fun dibujar(view: View) {
        val tareas = AgendaDb.obtener(requireContext()).listarTareas()
        val fechasConTareas = tareas.map { it.fecha }.toSet()
        val hoy = Fechas.hoy()

        view.findViewById<TextView>(R.id.txtMes).text = Fechas.mesAnio(mes)

        val cuadricula = view.findViewById<GridLayout>(R.id.gridDias)
        cuadricula.removeAllViews()
        for (letra in listOf("L", "M", "X", "J", "V", "S", "D")) {
            cuadricula.addView(crearCelda(letra, true))
        }

        // La semana empieza en lunes, por eso se corre el primer día del mes
        val desfase = (mes.get(Calendar.DAY_OF_WEEK) + 5) % 7
        repeat(desfase) { cuadricula.addView(crearCelda("", false)) }

        val prefijo = String.format(Locale.US, "%04d-%02d-", mes.get(Calendar.YEAR), mes.get(Calendar.MONTH) + 1)
        for (dia in 1..mes.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            val fecha = prefijo + String.format(Locale.US, "%02d", dia)
            val celda = crearCelda(dia.toString(), false)

            when {
                fecha == diaElegido -> {
                    celda.setBackgroundResource(R.drawable.bg_chip_sel)
                    celda.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                fecha == hoy -> {
                    celda.setBackgroundResource(R.drawable.bg_dia_sel)
                    celda.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                fecha in fechasConTareas -> celda.setBackgroundResource(R.drawable.bg_dia)
            }

            celda.setOnClickListener {
                diaElegido = if (diaElegido == fecha) null else fecha
                dibujar(view)
            }
            cuadricula.addView(celda)
        }

        val dia = diaElegido
        val lista = if (dia == null) {
            tareas.filter { !it.hecha }.take(3)
        } else {
            tareas.filter { it.fecha == dia }
        }
        view.findViewById<TextView>(R.id.txtTituloDia).text =
            if (dia == null) "Próximas entregas" else "Tareas del ${Fechas.legible(dia)}"
        val vacio = if (dia == null) "No tienes entregas pendientes." else "No hay tareas para este día."
        mostrarTareas(view.findViewById<LinearLayout>(R.id.listaDia), lista, vacio) { dibujar(view) }
    }

    private fun crearCelda(texto: String, esEncabezado: Boolean): TextView {
        val celda = TextView(requireContext())
        celda.text = texto
        celda.gravity = Gravity.CENTER
        celda.textSize = if (esEncabezado) 13f else 16f
        val color = if (esEncabezado) R.color.mute else R.color.navy
        celda.setTextColor(ContextCompat.getColor(requireContext(), color))
        val params = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        )
        params.width = 0
        params.height = (46 * resources.displayMetrics.density).toInt()
        params.setMargins(3, 3, 3, 3)
        celda.layoutParams = params
        return celda
    }
}
