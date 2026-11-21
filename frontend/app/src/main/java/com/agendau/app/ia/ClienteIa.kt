package com.agendau.app.ia

import com.agendau.app.BuildConfig
import com.agendau.app.util.Fechas
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

// Error con un mensaje pensado para mostrarse directamente al usuario
open class ErrorIa(mensaje: String) : Exception(mensaje)

// Error pasajero del servicio (saturado o límite de uso): vale la pena reintentar
class ErrorIaTemporal(mensaje: String) : ErrorIa(mensaje)

// Tarea que la IA extrae a partir de un texto escrito por el estudiante
data class TareaSugerida(
    val titulo: String,
    val materia: String,
    val fecha: String,
    val hora: String,
    val prioridad: Int
)

// Conexión con la API de Gemini (Google). Se usa la API REST, sin librerías externas
object ClienteIa {

    private const val URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/"
    private const val INTENTOS = 3

    // ---------- Funcionalidades de IA ----------

    fun interpretarTarea(texto: String, materias: List<String>): TareaSugerida {
        val hoy = Fechas.hoy()
        val instruccion = "Eres el asistente de AgendaU, una agenda para estudiantes universitarios. " +
            "Hoy es ${Fechas.legible(hoy)} (formato yyyy-MM-dd: $hoy). " +
            "Materias del estudiante: ${materias.joinToString(", ")}. " +
            "Del texto del estudiante extrae UNA tarea y responde SOLO con un objeto JSON con estas claves: " +
            "\"titulo\" (texto corto, máximo 60 caracteres), " +
            "\"materia\" (exactamente uno de los nombres de la lista, o \"\" si no coincide ninguno), " +
            "\"fecha\" (formato yyyy-MM-dd; resuelve expresiones como mañana o el viernes usando la fecha de hoy), " +
            "\"hora\" (formato HH:mm en 24 horas, o \"23:59\" si no se indica), " +
            "\"prioridad\" (0 baja, 1 media, 2 alta). " +
            "Si el texto no describe ninguna tarea, responde {\"error\": \"explicación breve\"}."

        val respuesta = preguntar(instruccion + "\n\nTexto del estudiante: " + texto, true)
        return leerTarea(respuesta)
    }

    fun generarPlan(tareasPendientes: List<String>): String {
        val hoy = Fechas.hoy()
        val instruccion = "Eres un asistente académico de la aplicación AgendaU. " +
            "Hoy es ${Fechas.legible(hoy)}. " +
            "A continuación están las tareas pendientes del estudiante (materia, título, fecha, hora y prioridad). " +
            "Crea un plan de estudio breve y realista para los próximos 7 días: " +
            "escribe un día por línea (por ejemplo \"Martes 17: ...\"), empieza por lo más urgente o de mayor prioridad " +
            "y cierra con un consejo corto. Responde en español, con máximo 150 palabras y sin usar markdown."

        val respuesta = preguntar(instruccion + "\n\nTareas pendientes:\n" + tareasPendientes.joinToString("\n"), false)
        return limpiar(respuesta)
    }

    // ---------- Comunicación con el servicio ----------

    private fun preguntar(mensaje: String, comoJson: Boolean): String {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            throw ErrorIa("Falta la clave de la API. Agrégala en el archivo local.properties (ver README).")
        }

        val parte = JSONObject().put("text", mensaje)
        val contenido = JSONObject().put("role", "user").put("parts", JSONArray().put(parte))
        val configuracion = JSONObject().put("temperature", 0.3)
        if (comoJson) {
            configuracion.put("responseMimeType", "application/json")
        }
        val cuerpo = JSONObject()
            .put("contents", JSONArray().put(contenido))
            .put("generationConfig", configuracion)
            .toString()

        // Si el servicio está saturado se reintenta unas veces antes de mostrar el error
        var ultimoError: ErrorIa? = null
        for (intento in 1..INTENTOS) {
            try {
                return enviar(cuerpo)
            } catch (e: ErrorIaTemporal) {
                ultimoError = e
                if (intento < INTENTOS) {
                    Thread.sleep(2000L * intento)
                }
            }
        }
        throw ultimoError ?: ErrorIa("No se pudo obtener respuesta del servicio de IA.")
    }

    private fun enviar(cuerpo: String): String {
        val url = URL(URL_BASE + BuildConfig.GEMINI_MODEL + ":generateContent")
        val conexion = url.openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.connectTimeout = 15000
            conexion.readTimeout = 60000
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json")
            conexion.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)

            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }

            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (codigo !in 200..299) {
                throw errorDeServicio(codigo, texto)
            }
            return extraerTexto(texto)
        } catch (e: UnknownHostException) {
            throw ErrorIa("Sin conexión a internet. Revisa tu red e inténtalo de nuevo.")
        } catch (e: SocketTimeoutException) {
            throw ErrorIa("El servicio tardó demasiado en responder. Inténtalo de nuevo.")
        } catch (e: IOException) {
            throw ErrorIa("No se pudo comunicar con el servicio de IA.")
        } finally {
            conexion.disconnect()
        }
    }

    private fun errorDeServicio(codigo: Int, cuerpo: String): ErrorIa {
        val detalle = try {
            JSONObject(cuerpo).getJSONObject("error").getString("message").take(160)
        } catch (e: Exception) {
            ""
        }
        return when (codigo) {
            400 -> ErrorIa("El servicio rechazó la solicitud (400). $detalle")
            401, 403 -> ErrorIa("La clave de la API no es válida o no tiene permisos (código $codigo). $detalle")
            404 -> ErrorIa("No se encontró el modelo de IA configurado (404). Revisa GEMINI_MODEL en local.properties.")
            429 -> ErrorIaTemporal("Se alcanzó el límite de uso del servicio (429). Espera un momento e inténtalo de nuevo. $detalle")
            in 500..599 -> ErrorIaTemporal("El servicio de IA no está disponible en este momento (código $codigo). $detalle")
            else -> ErrorIa("Error del servicio de IA (código $codigo). $detalle")
        }
    }

    private fun extraerTexto(respuesta: String): String {
        try {
            val candidatos = JSONObject(respuesta).optJSONArray("candidates")
            if (candidatos == null || candidatos.length() == 0) {
                throw ErrorIa("El servicio no devolvió una respuesta. Intenta reformular tu solicitud.")
            }
            val partes = candidatos.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
            if (partes == null || partes.length() == 0) {
                throw ErrorIa("La respuesta de la IA llegó vacía. Inténtalo de nuevo.")
            }
            val texto = StringBuilder()
            for (i in 0 until partes.length()) {
                texto.append(partes.getJSONObject(i).optString("text", ""))
            }
            val resultado = texto.toString().trim()
            if (resultado.isEmpty()) {
                throw ErrorIa("La respuesta de la IA llegó vacía. Inténtalo de nuevo.")
            }
            return resultado
        } catch (e: JSONException) {
            throw ErrorIa("No se pudo leer la respuesta del servicio de IA.")
        }
    }

    // ---------- Procesamiento de las respuestas ----------

    private fun leerTarea(respuesta: String): TareaSugerida {
        // La IA a veces envuelve el JSON en bloques de código, por eso se recorta entre llaves
        val inicio = respuesta.indexOf('{')
        val fin = respuesta.lastIndexOf('}')
        if (inicio < 0 || fin <= inicio) {
            throw ErrorIa("La respuesta de la IA no tiene el formato esperado. Inténtalo de nuevo.")
        }
        val json = try {
            JSONObject(respuesta.substring(inicio, fin + 1))
        } catch (e: JSONException) {
            throw ErrorIa("No se pudo interpretar la respuesta de la IA. Inténtalo de nuevo.")
        }

        val mensajeError = json.optString("error", "")
        if (mensajeError.isNotBlank()) {
            throw ErrorIa("La IA no encontró una tarea en el texto: $mensajeError")
        }

        val fecha = json.optString("fecha", "")
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(fecha)) {
            throw ErrorIa("La IA no pudo identificar la fecha. Incluye el día en tu texto.")
        }
        var hora = json.optString("hora", "23:59")
        if (!Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(hora)) {
            hora = "23:59"
        }

        return TareaSugerida(
            titulo = json.optString("titulo", "").trim().take(60),
            materia = json.optString("materia", "").trim(),
            fecha = fecha,
            hora = hora,
            prioridad = json.optInt("prioridad", 1).coerceIn(0, 2)
        )
    }

    private fun limpiar(texto: String): String =
        texto.replace("**", "")
            .replace(Regex("(?m)^\\s*\\*\\s+"), "- ")
            .replace("#", "")
            .trim()
}
