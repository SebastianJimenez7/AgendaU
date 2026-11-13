package com.agendau.app.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TareasFragment : Fragment(R.layout.fragment_tareas) {

    // 0 = todas, 1 = pendientes, 2 = hechas
    private var filtro = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val chips = listOf(
            view.findViewById<TextView>(R.id.chipTodas),
            view.findViewById<TextView>(R.id.chipPendientes),
            view.findViewById<TextView>(R.id.chipHechas)
        )
        chips.forEachIndexed { i, chip ->
            chip.setOnClickListener {
                filtro = i
                mostrar(view)
            }
        }

        view.findViewById<FloatingActionButton>(R.id.btnNuevaTarea).setOnClickListener {
            mostrarFormularioTarea(requireContext()) { mostrar(view) }
        }
        mostrar(view)
    }

    private fun mostrar(view: View) {
        val chips = listOf(R.id.chipTodas, R.id.chipPendientes, R.id.chipHechas)
        for ((i, id) in chips.withIndex()) {
            val chip = view.findViewById<TextView>(id)
            val activo = i == filtro
            chip.setBackgroundResource(if (activo) R.drawable.bg_chip_sel else R.drawable.bg_chip)
            chip.setTextColor(
                ContextCompat.getColor(requireContext(), if (activo) R.color.white else R.color.navy)
            )
        }

        val todas = AgendaDb.obtener(requireContext()).listarTareas()
        val lista = when (filtro) {
            1 -> todas.filter { !it.hecha }
            2 -> todas.filter { it.hecha }
            else -> todas
        }
        val vacio = when (filtro) {
            1 -> "No tienes tareas pendientes."
            2 -> "Todavía no has terminado ninguna tarea."
            else -> "Todavía no hay tareas. Pulsa + para agregar la primera."
        }
        mostrarTareas(view.findViewById<LinearLayout>(R.id.listaTareas), lista, vacio) { mostrar(view) }
    }
}
