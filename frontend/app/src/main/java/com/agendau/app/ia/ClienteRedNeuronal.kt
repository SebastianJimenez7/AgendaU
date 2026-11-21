package com.agendau.app.ia

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

// Respuesta de la red neuronal para una tarea
data class Prediccion(
    val probabilidad: Double,
    val riesgo: String,
    val recomendacion: String
)

// Cliente de la API en Python (Flask) que ejecuta la red neuronal entrenada con Keras
object ClienteRedNeuronal {

    // 10.0.2.2 es la dirección con la que el emulador de Android llega al localhost del computador
    private const val URL_SERVIDOR = "http://10.0.2.2:5000/predecir"

    fun predecir(
        diasRestantes: Int,
        horasPlaneadas: Double,
        prioridad: Int,
        tareasPendientes: Int
    ): Prediccion {
        val cuerpo = JSONObject()
            .put("dias_restantes", diasRestantes)
            .put("horas_planeadas", horasPlaneadas)
            .put("prioridad", prioridad)
            .put("tareas_pendientes", tareasPendientes)

        val conexion = URL(URL_SERVIDOR).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 15000
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }

            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (codigo !in 200..299) {
                val detalle = try {
                    JSONObject(texto).getString("error")
                } catch (e: Exception) {
                    "código $codigo"
                }
                throw ErrorIa("El servidor de la red neuronal rechazó los datos: $detalle")
            }

            val json = JSONObject(texto)
            return Prediccion(
                json.getDouble("probabilidad"),
                json.getString("riesgo"),
                json.optString("recomendacion", "")
            )
        } catch (e: ConnectException) {
            throw ErrorIa("No se pudo conectar con el servidor de la red neuronal. Verifica que esté en ejecución (python api.py).")
        } catch (e: SocketTimeoutException) {
            throw ErrorIa("El servidor de la red neuronal tardó demasiado en responder.")
        } catch (e: JSONException) {
            throw ErrorIa("El servidor de la red neuronal envió una respuesta inesperada.")
        } catch (e: IOException) {
            throw ErrorIa("No se pudo comunicar con el servidor de la red neuronal.")
        } finally {
            conexion.disconnect()
        }
    }
}
