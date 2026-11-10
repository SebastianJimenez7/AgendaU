package com.agendau.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class InicioFragment : Fragment(R.layout.fragment_inicio) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pendientes = tareasEjemplo.filter { !it.hecha }
        view.findViewById<TextView>(R.id.txtResumen).text =
            "Tienes ${pendientes.size} entregas pendientes"

        val franja = view.findViewById<LinearLayout>(R.id.franjaDias)
        val dias = listOf("S" to 7, "D" to 8, "L" to 9, "M" to 10, "X" to 11)
        for ((letra, numero) in dias) {
            val dia = TextView(requireContext())
            dia.text = "$letra\n$numero"
            dia.gravity = Gravity.CENTER
            dia.textSize = 16f
            val hoy = numero == 7
            dia.setBackgroundResource(if (hoy) R.drawable.bg_dia_sel else R.drawable.bg_dia)
            dia.setTextColor(ContextCompat.getColor(requireContext(), if (hoy) R.color.white else R.color.navy))
            val params = LinearLayout.LayoutParams(0, (72 * resources.displayMetrics.density).toInt(), 1f)
            params.setMargins(4, 0, 4, 0)
            dia.layoutParams = params
            franja.addView(dia)
        }

        llenarTareas(view.findViewById(R.id.listaPendientes), pendientes)

        view.findViewById<FloatingActionButton>(R.id.btnNueva).setOnClickListener {
            Toast.makeText(requireContext(), "Aquí se va a crear una tarea nueva", Toast.LENGTH_SHORT).show()
        }
    }
}
