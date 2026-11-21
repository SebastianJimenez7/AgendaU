package com.agendau.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.agendau.app.R
import com.agendau.app.data.AgendaDb
import com.agendau.app.data.Tarea
import com.agendau.app.ia.ClienteIa
import com.agendau.app.ia.ClienteRedNeuronal
import com.agendau.app.ia.ErrorIa
import com.agendau.app.ia.TareaSugerida
import com.agendau.app.util.Fechas
import com.agendau.app.util.Validaciones
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread

class AsistenteFragment : Fragment(R.layout.fragment_asistente) {

    private var tareaSugerida: TareaSugerida? = null
    private var materiaSugeridaId: Long = -1
    private var pendientesRiesgo: List<Tarea> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btnInterpretar).setOnClickListener { interpretar(view) }
        view.findViewById<Button>(R.id.btnGuardarIa).setOnClickListener { guardar(view) }
        view.findViewById<Button>(R.id.btnDescartarIa).setOnClickListener { ocultarResultado(view) }
        view.findViewById<Button>(R.id.btnPlan).setOnClickListener { generarPlan(view) }
        view.findViewById<Button>(R.id.btnRiesgo).setOnClickListener { calcularRiesgo(view) }
        cargarTareasRiesgo(view)
    }

    // ---------- Crear tarea con IA ----------

    private fun interpretar(view: View) {
        val materias = AgendaDb.obtener(requireContext()).listarMaterias()
        val til = view.findViewById<TextInputLayout>(R.id.tilTextoIa)
        val texto = view.findViewById<TextInputEditText>(R.id.edtTextoIa).text.toString().trim()

        // Validaciones antes de llamar al servicio
        til.error = null
        ocultarResultado(view)
        mostrarError(view, R.id.txtErrorTarea, null)
        if (materias.isEmpty()) {
            mostrarError(view, R.id.txtErrorTarea, "Primero crea una materia en la pestaña Materias para que la IA pueda asignarla.")
            return
        }
        if (texto.length < 5) {
            til.error = "Escribe una descripción más completa (mínimo 5 caracteres)"
            return
        }
        if (texto.length > 300) {
            til.error = "El texto es muy largo (máximo 300 caracteres)"
            return
        }

        ocupado(view, R.id.progresoTarea, R.id.btnInterpretar, true)
        val nombres = materias.map { it.nombre }

        // La llamada a internet se hace en un hilo aparte para no congelar la pantalla
        thread {
            try {
                val tarea = ClienteIa.interpretarTarea(texto, nombres)
                activity?.runOnUiThread {
                    if (isAdded) {
                        ocupado(view, R.id.progresoTarea, R.id.btnInterpretar, false)
                        mostrarSugerencia(view, tarea)
                    }
                }
            } catch (e: ErrorIa) {
                terminarConError(view, R.id.progresoTarea, R.id.btnInterpretar, R.id.txtErrorTarea, e.message)
            } catch (e: Exception) {
                terminarConError(view, R.id.progresoTarea, R.id.btnInterpretar, R.id.txtErrorTarea, null)
            }
        }
    }

    private fun mostrarSugerencia(view: View, tarea: TareaSugerida) {
        val materias = AgendaDb.obtener(requireContext()).listarMaterias()
        val materia = materias.firstOrNull { it.nombre.equals(tarea.materia, ignoreCase = true) }
        if (materia == null) {
            mostrarError(view, R.id.txtErrorTarea, "La IA no pudo asociar la tarea a una de tus materias. Menciona la materia en el texto.")
            return
        }

        // Los datos que devuelve la IA también pasan por las validaciones de la app
        val errorTitulo = Validaciones.validarTitulo(tarea.titulo)
        val errorFecha = Validaciones.validarFecha(tarea.fecha)
        if (errorTitulo != null || errorFecha != null) {
            mostrarError(view, R.id.txtErrorTarea, "La IA devolvió datos no válidos: ${errorTitulo ?: errorFecha}. Intenta describir mejor la tarea.")
            return
        }

        tareaSugerida = tarea
        materiaSugeridaId = materia.id
        view.findViewById<TextView>(R.id.txtResultado).text =
            "Título: ${tarea.titulo}\n" +
                "Materia: ${materia.nombre}\n" +
                "Fecha: ${Fechas.legible(tarea.fecha)}\n" +
                "Hora: ${Fechas.horaLegible(tarea.hora)}\n" +
                "Prioridad: ${NOMBRES_PRIORIDAD[tarea.prioridad]}"
        view.findViewById<LinearLayout>(R.id.panelResultado).visibility = View.VISIBLE
    }

    private fun guardar(view: View) {
        val tarea = tareaSugerida ?: return
        AgendaDb.obtener(requireContext())
            .insertarTarea(materiaSugeridaId, tarea.titulo, tarea.fecha, tarea.hora, tarea.prioridad)
        Toast.makeText(requireContext(), "Tarea guardada", Toast.LENGTH_SHORT).show()
        view.findViewById<TextInputEditText>(R.id.edtTextoIa).setText("")
        ocultarResultado(view)
        cargarTareasRiesgo(view)
    }

    private fun ocultarResultado(view: View) {
        tareaSugerida = null
        view.findViewById<LinearLayout>(R.id.panelResultado).visibility = View.GONE
    }

    // ---------- Plan de estudio ----------

    private fun generarPlan(view: View) {
        val pendientes = AgendaDb.obtener(requireContext()).listarTareas().filter { !it.hecha }

        mostrarError(view, R.id.txtErrorPlan, null)
        view.findViewById<TextView>(R.id.txtPlan).visibility = View.GONE
        if (pendientes.isEmpty()) {
            mostrarError(view, R.id.txtErrorPlan, "No tienes tareas pendientes para planificar.")
            return
        }

        val lineas = pendientes.map {
            "- ${it.materiaNombre}: ${it.titulo}, ${it.fecha} ${it.hora}, prioridad ${NOMBRES_PRIORIDAD[it.prioridad].lowercase()}"
        }

        ocupado(view, R.id.progresoPlan, R.id.btnPlan, true)
        thread {
            try {
                val plan = ClienteIa.generarPlan(lineas)
                activity?.runOnUiThread {
                    if (isAdded) {
                        ocupado(view, R.id.progresoPlan, R.id.btnPlan, false)
                        val txtPlan = view.findViewById<TextView>(R.id.txtPlan)
                        txtPlan.text = plan
                        txtPlan.visibility = View.VISIBLE
                    }
                }
            } catch (e: ErrorIa) {
                terminarConError(view, R.id.progresoPlan, R.id.btnPlan, R.id.txtErrorPlan, e.message)
            } catch (e: Exception) {
                terminarConError(view, R.id.progresoPlan, R.id.btnPlan, R.id.txtErrorPlan, null)
            }
        }
    }

    // ---------- Riesgo de entrega (red neuronal en Python) ----------

    private fun cargarTareasRiesgo(view: View) {
        pendientesRiesgo = AgendaDb.obtener(requireContext()).listarTareas().filter { !it.hecha }
        val nombres = pendientesRiesgo.map { "${it.materiaNombre}: ${it.titulo}" }
        val lista = if (nombres.isEmpty()) listOf("No hay tareas pendientes") else nombres
        view.findViewById<Spinner>(R.id.spnTareaRiesgo).adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, lista)
    }

    private fun calcularRiesgo(view: View) {
        val til = view.findViewById<TextInputLayout>(R.id.tilHoras)
        val textoHoras = view.findViewById<TextInputEditText>(R.id.edtHoras).text.toString().trim()

        til.error = null
        mostrarError(view, R.id.txtErrorRiesgo, null)
        view.findViewById<TextView>(R.id.txtRiesgo).visibility = View.GONE

        // Validaciones antes de llamar al servidor
        if (pendientesRiesgo.isEmpty()) {
            mostrarError(view, R.id.txtErrorRiesgo, "No tienes tareas pendientes para analizar.")
            return
        }
        if (textoHoras.isEmpty()) {
            til.error = "Escribe cuántas horas planeas dedicar"
            return
        }
        val horas = textoHoras.replace(',', '.').toDoubleOrNull()
        if (horas == null) {
            til.error = "Escribe un número válido"
            return
        }
        if (horas < 0 || horas > 24) {
            til.error = "Las horas deben estar entre 0 y 24"
            return
        }

        val tarea = pendientesRiesgo[view.findViewById<Spinner>(R.id.spnTareaRiesgo).selectedItemPosition]
        val dias = Fechas.diasHasta(tarea.fecha).coerceAtLeast(0)
        val pendientes = pendientesRiesgo.size

        ocupado(view, R.id.progresoRiesgo, R.id.btnRiesgo, true)
        thread {
            try {
                val prediccion = ClienteRedNeuronal.predecir(dias, horas, tarea.prioridad, pendientes)
                activity?.runOnUiThread {
                    if (isAdded) {
                        ocupado(view, R.id.progresoRiesgo, R.id.btnRiesgo, false)
                        val porcentaje = Math.round(prediccion.probabilidad * 100)
                        val txt = view.findViewById<TextView>(R.id.txtRiesgo)
                        txt.text = "Probabilidad de entregar a tiempo: $porcentaje %\n" +
                            "Riesgo: ${prediccion.riesgo}\n${prediccion.recomendacion}"
                        txt.visibility = View.VISIBLE
                    }
                }
            } catch (e: ErrorIa) {
                terminarConError(view, R.id.progresoRiesgo, R.id.btnRiesgo, R.id.txtErrorRiesgo, e.message)
            } catch (e: Exception) {
                terminarConError(view, R.id.progresoRiesgo, R.id.btnRiesgo, R.id.txtErrorRiesgo, null)
            }
        }
    }

    // ---------- Apoyo para la interfaz ----------

    private fun ocupado(view: View, idProgreso: Int, idBoton: Int, ocupado: Boolean) {
        view.findViewById<View>(idProgreso).visibility = if (ocupado) View.VISIBLE else View.GONE
        view.findViewById<Button>(idBoton).isEnabled = !ocupado
    }

    private fun mostrarError(view: View, idTexto: Int, mensaje: String?) {
        val texto = view.findViewById<TextView>(idTexto)
        texto.text = mensaje ?: ""
        texto.visibility = if (mensaje == null) View.GONE else View.VISIBLE
    }

    private fun terminarConError(view: View, idProgreso: Int, idBoton: Int, idError: Int, mensaje: String?) {
        activity?.runOnUiThread {
            if (isAdded) {
                ocupado(view, idProgreso, idBoton, false)
                mostrarError(view, idError, mensaje ?: "Ocurrió un error inesperado. Inténtalo de nuevo.")
            }
        }
    }
}
