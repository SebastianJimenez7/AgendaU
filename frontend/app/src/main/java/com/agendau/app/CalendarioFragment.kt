package com.agendau.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class CalendarioFragment : Fragment(R.layout.fragment_calendario) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cuadricula = view.findViewById<GridLayout>(R.id.gridDias)

        for (letra in listOf("L", "M", "X", "J", "V", "S", "D")) {
            cuadricula.addView(crearCelda(letra, true))
        }
        // Noviembre de 2026 empieza en domingo, por eso hay 6 espacios vacíos
        repeat(6) { cuadricula.addView(crearCelda("", false)) }

        val conEntregas = setOf(9, 10, 13)
        for (dia in 1..30) {
            val celda = crearCelda(dia.toString(), false)
            if (dia == 7) {
                celda.setBackgroundResource(R.drawable.bg_dia_sel)
                celda.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else if (dia in conEntregas) {
                celda.setBackgroundResource(R.drawable.bg_dia)
            }
            cuadricula.addView(celda)
        }

        val proximas = tareasEjemplo.filter { !it.hecha }.take(3)
        llenarTareas(view.findViewById<LinearLayout>(R.id.listaProximas), proximas)
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
