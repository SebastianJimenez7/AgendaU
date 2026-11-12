package com.agendau.app.data

data class Materia(
    val id: Long,
    val nombre: String,
    val horario: String,
    val color: Int
)

data class Tarea(
    val id: Long,
    val materiaId: Long,
    val materiaNombre: String,
    val colorMateria: Int,
    val titulo: String,
    val fecha: String,      // formato yyyy-MM-dd
    val hora: String,       // formato HH:mm
    val prioridad: Int,     // 0 baja, 1 media, 2 alta
    val hecha: Boolean
)
