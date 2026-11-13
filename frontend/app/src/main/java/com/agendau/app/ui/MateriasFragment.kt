package com.agendau.app.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MateriasFragment : Fragment(R.layout.fragment_materias) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<FloatingActionButton>(R.id.btnNuevaMateria).setOnClickListener {
            mostrarFormularioMateria(requireContext()) { mostrar(view) }
        }
        mostrar(view)
    }

    private fun mostrar(view: View) {
        val materias = AgendaDb.obtener(requireContext()).listarMaterias()
        mostrarMaterias(view.findViewById<LinearLayout>(R.id.listaMaterias), materias) { mostrar(view) }
    }
}
