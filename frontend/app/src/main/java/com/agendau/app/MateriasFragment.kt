package com.agendau.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class MateriasFragment : Fragment(R.layout.fragment_materias) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        llenarMaterias(view.findViewById<LinearLayout>(R.id.listaMaterias), materiasEjemplo)
    }
}
