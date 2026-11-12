package com.agendau.app.ui

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.agendau.app.data.Materia
import com.agendau.app.data.Tarea
import com.agendau.app.util.Fechas
import com.google.android.material.dialog.MaterialAlertDialogBuilder

val COLORES_MATERIA = listOf(R.color.orange, R.color.aqua, R.color.blue, R.color.navy)
val NOMBRES_COLOR = listOf("Naranja", "Turquesa", "Azul", "Azul oscuro")
val NOMBRES_PRIORIDAD = listOf("Baja", "Media", "Alta")
private val COLORES_PRIORIDAD = listOf(R.color.aqua, R.color.orange, R.color.red)

fun colorDe(contexto: Context, indice: Int): Int =
    ContextCompat.getColor(contexto, COLORES_MATERIA[indice.coerceIn(0, COLORES_MATERIA.size - 1)])

private fun mensajeVacio(contexto: Context, texto: String): TextView {
    val vista = TextView(contexto)
    vista.text = texto
    vista.textSize = 15f
    vista.setTextColor(ContextCompat.getColor(contexto, R.color.mute))
    vista.setPadding(0, 16, 0, 16)
    return vista
}

fun mostrarTareas(
    contenedor: LinearLayout,
    lista: List<Tarea>,
    textoVacio: String,
    alCambiar: () -> Unit
) {
    val contexto = contenedor.context
    val db = AgendaDb.obtener(contexto)
    val inflador = LayoutInflater.from(contexto)
    contenedor.removeAllViews()

    if (lista.isEmpty()) {
        contenedor.addView(mensajeVacio(contexto, textoVacio))
        return
    }

    for (t in lista) {
        val item = inflador.inflate(R.layout.item_tarea, contenedor, false)

        val titulo = item.findViewById<TextView>(R.id.txtTitulo)
        titulo.text = t.titulo
        if (t.hecha) {
            titulo.paintFlags = titulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        val vencida = !t.hecha && Fechas.esAnteriorAHoy(t.fecha)
        val detalle = "${t.materiaNombre} · ${Fechas.etiquetaDia(t.fecha)}, ${Fechas.horaLegible(t.hora)}"
        item.findViewById<TextView>(R.id.txtDetalle).text = if (vencida) "$detalle · Vencida" else detalle

        val prioridad = item.findViewById<TextView>(R.id.txtPrioridad)
        prioridad.text = "Prioridad ${NOMBRES_PRIORIDAD[t.prioridad].lowercase()}"
        prioridad.setTextColor(ContextCompat.getColor(contexto, COLORES_PRIORIDAD[t.prioridad]))

        item.findViewById<View>(R.id.barra).setBackgroundColor(colorDe(contexto, t.colorMateria))

        val casilla = item.findViewById<CheckBox>(R.id.chkHecha)
        casilla.isChecked = t.hecha
        casilla.setOnCheckedChangeListener { _, marcada ->
            db.marcarHecha(t.id, marcada)
            contenedor.post { alCambiar() }
        }

        item.findViewById<ImageButton>(R.id.btnBorrar).setOnClickListener {
            MaterialAlertDialogBuilder(contexto)
                .setTitle("Eliminar tarea")
                .setMessage("¿Quieres eliminar \"${t.titulo}\"?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    db.eliminarTarea(t.id)
                    alCambiar()
                }
                .show()
        }

        contenedor.addView(item)
    }
}

fun mostrarMaterias(contenedor: LinearLayout, lista: List<Materia>, alCambiar: () -> Unit) {
    val contexto = contenedor.context
    val db = AgendaDb.obtener(contexto)
    val inflador = LayoutInflater.from(contexto)
    contenedor.removeAllViews()

    if (lista.isEmpty()) {
        contenedor.addView(mensajeVacio(contexto, "Todavía no hay materias. Pulsa + para agregar la primera."))
        return
    }

    for (m in lista) {
        val item = inflador.inflate(R.layout.item_materia, contenedor, false)

        val inicial = item.findViewById<TextView>(R.id.txtInicial)
        inicial.text = m.nombre.take(1).uppercase()
        inicial.setBackgroundColor(colorDe(contexto, m.color))

        item.findViewById<TextView>(R.id.txtNombre).text = m.nombre
        item.findViewById<TextView>(R.id.txtHorario).text = m.horario

        val pendientes = db.contarPendientes(m.id)
        item.findViewById<TextView>(R.id.txtPendientes).text = when (pendientes) {
            0 -> "Sin tareas pendientes"
            1 -> "1 tarea pendiente"
            else -> "$pendientes tareas pendientes"
        }

        item.findViewById<ImageButton>(R.id.btnBorrarMateria).setOnClickListener {
            MaterialAlertDialogBuilder(contexto)
                .setTitle("Eliminar materia")
                .setMessage("Se eliminará \"${m.nombre}\" y también todas sus tareas. ¿Continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    db.eliminarMateria(m.id)
                    alCambiar()
                }
                .show()
        }

        contenedor.addView(item)
    }
}
