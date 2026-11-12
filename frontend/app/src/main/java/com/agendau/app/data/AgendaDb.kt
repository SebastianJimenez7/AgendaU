package com.agendau.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AgendaDb private constructor(contexto: Context) :
    SQLiteOpenHelper(contexto.applicationContext, "agendau.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE materias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "horario TEXT NOT NULL, " +
                "color INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE tareas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "materia_id INTEGER NOT NULL, " +
                "titulo TEXT NOT NULL, " +
                "fecha TEXT NOT NULL, " +
                "hora TEXT NOT NULL, " +
                "prioridad INTEGER NOT NULL, " +
                "hecha INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (materia_id) REFERENCES materias(id) ON DELETE CASCADE)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
        db.execSQL("DROP TABLE IF EXISTS tareas")
        db.execSQL("DROP TABLE IF EXISTS materias")
        onCreate(db)
    }

    // ---------- Materias ----------

    fun insertarMateria(nombre: String, horario: String, color: Int): Long {
        val valores = ContentValues().apply {
            put("nombre", nombre)
            put("horario", horario)
            put("color", color)
        }
        return writableDatabase.insert("materias", null, valores)
    }

    fun listarMaterias(): List<Materia> {
        val lista = mutableListOf<Materia>()
        val consulta = "SELECT id, nombre, horario, color FROM materias ORDER BY nombre COLLATE NOCASE"
        readableDatabase.rawQuery(consulta, null).use { c ->
            while (c.moveToNext()) {
                lista.add(Materia(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3)))
            }
        }
        return lista
    }

    fun existeMateria(nombre: String): Boolean {
        val consulta = "SELECT 1 FROM materias WHERE LOWER(nombre) = LOWER(?)"
        return readableDatabase.rawQuery(consulta, arrayOf(nombre)).use { it.moveToFirst() }
    }

    fun eliminarMateria(id: Long) {
        // Las tareas de la materia se borran solas por el ON DELETE CASCADE
        writableDatabase.delete("materias", "id = ?", arrayOf(id.toString()))
    }

    fun contarPendientes(materiaId: Long): Int {
        val consulta = "SELECT COUNT(*) FROM tareas WHERE materia_id = ? AND hecha = 0"
        return readableDatabase.rawQuery(consulta, arrayOf(materiaId.toString())).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    // ---------- Tareas ----------

    fun insertarTarea(materiaId: Long, titulo: String, fecha: String, hora: String, prioridad: Int): Long {
        val valores = ContentValues().apply {
            put("materia_id", materiaId)
            put("titulo", titulo)
            put("fecha", fecha)
            put("hora", hora)
            put("prioridad", prioridad)
            put("hecha", 0)
        }
        return writableDatabase.insert("tareas", null, valores)
    }

    fun listarTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val consulta = "SELECT t.id, t.materia_id, m.nombre, m.color, t.titulo, t.fecha, t.hora, " +
            "t.prioridad, t.hecha FROM tareas t JOIN materias m ON m.id = t.materia_id " +
            "ORDER BY t.hecha, t.fecha, t.hora"
        readableDatabase.rawQuery(consulta, null).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Tarea(
                        c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3),
                        c.getString(4), c.getString(5), c.getString(6), c.getInt(7), c.getInt(8) == 1
                    )
                )
            }
        }
        return lista
    }

    fun marcarHecha(id: Long, hecha: Boolean) {
        val valores = ContentValues().apply { put("hecha", if (hecha) 1 else 0) }
        writableDatabase.update("tareas", valores, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarTarea(id: Long) {
        writableDatabase.delete("tareas", "id = ?", arrayOf(id.toString()))
    }

    companion object {
        @Volatile
        private var instancia: AgendaDb? = null

        fun obtener(contexto: Context): AgendaDb =
            instancia ?: synchronized(this) {
                instancia ?: AgendaDb(contexto).also { instancia = it }
            }
    }
}
