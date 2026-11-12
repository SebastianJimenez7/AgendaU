package com.agendau.app.util

// Cada función devuelve el mensaje de error, o null si el dato es válido
object Validaciones {

    fun validarTitulo(titulo: String): String? = when {
        titulo.isBlank() -> "Escribe el título de la tarea"
        titulo.length < 3 -> "El título debe tener al menos 3 caracteres"
        titulo.length > 60 -> "El título es muy largo (máximo 60 caracteres)"
        else -> null
    }

    fun validarFecha(fecha: String?): String? = when {
        fecha == null -> "Elige la fecha de entrega"
        Fechas.esAnteriorAHoy(fecha) -> "La fecha no puede ser anterior a hoy"
        else -> null
    }

    fun validarNombreMateria(nombre: String, yaExiste: Boolean): String? = when {
        nombre.isBlank() -> "Escribe el nombre de la materia"
        nombre.length < 3 -> "El nombre debe tener al menos 3 caracteres"
        nombre.length > 40 -> "El nombre es muy largo (máximo 40 caracteres)"
        yaExiste -> "Ya tienes una materia con ese nombre"
        else -> null
    }

    fun validarHorario(horario: String): String? = when {
        horario.isBlank() -> "Escribe el horario de la materia"
        horario.length > 50 -> "El horario es muy largo (máximo 50 caracteres)"
        else -> null
    }
}
