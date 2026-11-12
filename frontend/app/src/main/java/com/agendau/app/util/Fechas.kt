package com.agendau.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Fechas {
    private val local: Locale = Locale.forLanguageTag("es-CO")
    private val formatoBd = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun aTexto(calendario: Calendar): String = formatoBd.format(calendario.time)

    fun hoy(): String = aTexto(Calendar.getInstance())

    // Como el formato es yyyy-MM-dd, comparar los textos equivale a comparar las fechas
    fun esAnteriorAHoy(fecha: String): Boolean = fecha < hoy()

    fun legible(fecha: String): String {
        val date = formatoBd.parse(fecha) ?: return fecha
        return SimpleDateFormat("EEEE d 'de' MMMM", local).format(date).replaceFirstChar { it.uppercase() }
    }

    fun legibleCorta(fecha: String): String {
        val date = formatoBd.parse(fecha) ?: return fecha
        return SimpleDateFormat("EEE d MMM", local).format(date).replaceFirstChar { it.uppercase() }
    }

    fun etiquetaDia(fecha: String): String {
        val manana = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        return when (fecha) {
            hoy() -> "Hoy"
            aTexto(manana) -> "Mañana"
            else -> legibleCorta(fecha)
        }
    }

    fun horaLegible(hora: String): String {
        val partes = hora.split(":")
        val h = partes[0].toInt()
        val minutos = partes[1]
        val sufijo = if (h < 12) "a. m." else "p. m."
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:$minutos $sufijo"
    }

    fun mesAnio(calendario: Calendar): String =
        SimpleDateFormat("MMMM yyyy", local).format(calendario.time).replaceFirstChar { it.uppercase() }
}
