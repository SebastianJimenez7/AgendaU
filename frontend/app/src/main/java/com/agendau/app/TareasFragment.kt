package com.agendau.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class TareasFragment : Fragment(R.layout.fragment_tareas) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lista = view.findViewById<LinearLayout>(R.id.listaTareas)
        val chips = listOf(
            view.findViewById<TextView>(R.id.chipTodas),
            view.findViewById<TextView>(R.id.chipPendientes),
            view.findViewById<TextView>(R.id.chipHechas)
        )

        fun filtrar(posicion: Int) {
            for ((i, chip) in chips.withIndex()) {
                val activo = i == posicion
                chip.setBackgroundResource(if (activo) R.drawable.bg_chip_sel else R.drawable.bg_chip)
                chip.setTextColor(ContextCompat.getColor(requireContext(), if (activo) R.color.white else R.color.navy))
            }
            val resultado = when (posicion) {
                1 -> tareasEjemplo.filter { !it.hecha }
                2 -> tareasEjemplo.filter { it.hecha }
                else -> tareasEjemplo
            }
            llenarTareas(lista, resultado)
        }

        chips.forEachIndexed { i, chip -> chip.setOnClickListener { filtrar(i) } }
        filtrar(0)
    }
}
