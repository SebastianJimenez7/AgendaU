package com.agendau.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.agendau.app.util.Fechas
import com.agendau.app.util.Validaciones
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import java.util.Locale

fun mostrarFormularioTarea(contexto: Context, alGuardar: () -> Unit) {
    val db = AgendaDb.obtener(contexto)
    val materias = db.listarMaterias()

    if (materias.isEmpty()) {
        MaterialAlertDialogBuilder(contexto)
            .setTitle("Primero crea una materia")
            .setMessage("Para registrar una tarea necesitas al menos una materia. Ve a la pestaña Materias y agrega una.")
            .setPositiveButton("Entendido", null)
            .show()
        return
    }

    val vista = LayoutInflater.from(contexto).inflate(R.layout.dialog_tarea, null)
    val tilTitulo = vista.findViewById<TextInputLayout>(R.id.tilTitulo)
    val edtTitulo = vista.findViewById<TextInputEditText>(R.id.edtTitulo)
    val spnMateria = vista.findViewById<Spinner>(R.id.spnMateria)
    val btnFecha = vista.findViewById<Button>(R.id.btnFecha)
    val btnHora = vista.findViewById<Button>(R.id.btnHora)
    val spnPrioridad = vista.findViewById<Spinner>(R.id.spnPrioridad)
    val txtError = vista.findViewById<TextView>(R.id.txtError)

    spnMateria.adapter = ArrayAdapter(
        contexto, android.R.layout.simple_spinner_dropdown_item, materias.map { it.nombre }
    )
    spnPrioridad.adapter = ArrayAdapter(
        contexto, android.R.layout.simple_spinner_dropdown_item, NOMBRES_PRIORIDAD
    )
    spnPrioridad.setSelection(1)

    var fechaElegida: String? = null
    var horaElegida = "23:59"

    btnFecha.setOnClickListener {
        val hoy = Calendar.getInstance()
        val selector = DatePickerDialog(
            contexto,
            { _, anio, mes, dia ->
                val elegida = Calendar.getInstance()
                elegida.set(anio, mes, dia)
                val texto = Fechas.aTexto(elegida)
                fechaElegida = texto
                btnFecha.text = Fechas.legible(texto)
            },
            hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)
        )
        selector.datePicker.minDate = System.currentTimeMillis() - 1000
        selector.show()
    }

    btnHora.setOnClickListener {
        TimePickerDialog(
            contexto,
            { _, hora, minutos ->
                horaElegida = String.format(Locale.US, "%02d:%02d", hora, minutos)
                btnHora.text = "Hora: ${Fechas.horaLegible(horaElegida)}"
            },
            23, 59, false
        ).show()
    }

    val dialogo = MaterialAlertDialogBuilder(contexto)
        .setTitle("Nueva tarea")
        .setView(vista)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Guardar", null)
        .create()

    // El botón se configura aparte para que el diálogo no se cierre si hay errores
    dialogo.setOnShowListener {
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val titulo = edtTitulo.text.toString().trim()
            val errorTitulo = Validaciones.validarTitulo(titulo)
            val errorFecha = Validaciones.validarFecha(fechaElegida)

            tilTitulo.error = errorTitulo
            txtError.text = errorFecha ?: ""
            txtError.visibility = if (errorFecha == null) View.GONE else View.VISIBLE

            val fecha = fechaElegida
            if (errorTitulo == null && errorFecha == null && fecha != null) {
                val materia = materias[spnMateria.selectedItemPosition]
                db.insertarTarea(materia.id, titulo, fecha, horaElegida, spnPrioridad.selectedItemPosition)
                dialogo.dismiss()
                alGuardar()
            }
        }
    }
    dialogo.show()
}

fun mostrarFormularioMateria(contexto: Context, alGuardar: () -> Unit) {
    val db = AgendaDb.obtener(contexto)
    val vista = LayoutInflater.from(contexto).inflate(R.layout.dialog_materia, null)
    val tilNombre = vista.findViewById<TextInputLayout>(R.id.tilNombre)
    val edtNombre = vista.findViewById<TextInputEditText>(R.id.edtNombre)
    val tilHorario = vista.findViewById<TextInputLayout>(R.id.tilHorario)
    val edtHorario = vista.findViewById<TextInputEditText>(R.id.edtHorario)
    val spnColor = vista.findViewById<Spinner>(R.id.spnColor)

    spnColor.adapter = ArrayAdapter(
        contexto, android.R.layout.simple_spinner_dropdown_item, NOMBRES_COLOR
    )

    val dialogo = MaterialAlertDialogBuilder(contexto)
        .setTitle("Nueva materia")
        .setView(vista)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Guardar", null)
        .create()

    dialogo.setOnShowListener {
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val nombre = edtNombre.text.toString().trim()
            val horario = edtHorario.text.toString().trim()
            val errorNombre = Validaciones.validarNombreMateria(nombre, db.existeMateria(nombre))
            val errorHorario = Validaciones.validarHorario(horario)

            tilNombre.error = errorNombre
            tilHorario.error = errorHorario

            if (errorNombre == null && errorHorario == null) {
                db.insertarMateria(nombre, horario, spnColor.selectedItemPosition)
                dialogo.dismiss()
                alGuardar()
            }
        }
    }
    dialogo.show()
}
