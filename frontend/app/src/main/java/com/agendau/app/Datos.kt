package com.agendau.app

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

data class Tarea(
    val materia: String,
    val titulo: String,
    val fecha: String,
    val color: Int,
    var hecha: Boolean = false
)

data class Materia(val nombre: String, val horario: String, val color: Int)

// Datos de ejemplo: por ahora no hay base de datos, solo sirven para ver cómo queda la interfaz
val tareasEjemplo = listOf(
    Tarea("Taller de Programación", "Subir el repositorio a GitHub", "Hoy, 11:59 p. m.", R.color.orange),
    Tarea("Computación Gráfica", "Modelar la taza en Blender", "Viernes 13", R.color.aqua),
    Tarea("Ecuaciones Diferenciales", "Taller de transformada de Laplace", "Lunes 9", R.color.blue),
    Tarea("Teoría de Bases de Datos", "Taller final de SQL", "Martes 10", R.color.gray),
    Tarea("Taller de Programación", "Ficha técnica del proyecto", "Entregada", R.color.orange, true)
)

val materiasEjemplo = listOf(
    Materia("Taller de Programación", "Lunes y miércoles · 7:00 a. m.", R.color.orange),
    Materia("Teoría de Bases de Datos", "Martes y jueves · 9:00 a. m.", R.color.aqua),
    Materia("Ecuaciones Diferenciales", "Lunes y viernes · 11:00 a. m.", R.color.blue),
    Materia("Computación Gráfica", "Miércoles · 2:00 p. m.", R.color.navy)
)

fun llenarTareas(contenedor: LinearLayout, lista: List<Tarea>) {
    val inflador = LayoutInflater.from(contenedor.context)
    contenedor.removeAllViews()
    for (t in lista) {
        val item = inflador.inflate(R.layout.item_tarea, contenedor, false)
        item.findViewById<TextView>(R.id.txtTitulo).text = t.titulo
        item.findViewById<TextView>(R.id.txtDetalle).text = "${t.materia} · ${t.fecha}"
        item.findViewById<View>(R.id.barra)
            .setBackgroundColor(ContextCompat.getColor(contenedor.context, t.color))
        val casilla = item.findViewById<CheckBox>(R.id.chkHecha)
        casilla.isChecked = t.hecha
        casilla.setOnCheckedChangeListener { _, marcada -> t.hecha = marcada }
        contenedor.addView(item)
    }
}

fun llenarMaterias(contenedor: LinearLayout, lista: List<Materia>) {
    val inflador = LayoutInflater.from(contenedor.context)
    contenedor.removeAllViews()
    for (m in lista) {
        val item = inflador.inflate(R.layout.item_materia, contenedor, false)
        val inicial = item.findViewById<TextView>(R.id.txtInicial)
        inicial.text = m.nombre.take(1)
        inicial.setBackgroundColor(ContextCompat.getColor(contenedor.context, m.color))
        item.findViewById<TextView>(R.id.txtNombre).text = m.nombre
        item.findViewById<TextView>(R.id.txtHorario).text = m.horario
        contenedor.addView(item)
    }
}
