"""
API de AgendaU para la red neuronal.

La app Android envía los datos de una tarea y la API responde con la
probabilidad de entregarla a tiempo. Para ejecutarla:

    python api.py

Desde el emulador de Android se accede con http://10.0.2.2:5000
"""
import json
import os
import sys

import numpy as np
from flask import Flask, jsonify, request

CARPETA = os.path.dirname(os.path.abspath(__file__))
RUTA_PESOS = os.path.join(CARPETA, "modelo", "pesos.json")

if not os.path.exists(RUTA_PESOS):
    print("No se encontró modelo/pesos.json. Primero ejecuta: python entrenar_modelo.py")
    sys.exit(1)

with open(RUTA_PESOS, encoding="utf-8") as archivo:
    PESOS = json.load(archivo)

VARIABLES = PESOS["variables"]
ESCALAS = PESOS["escalas"]
W1 = np.array(PESOS["capa_oculta"]["pesos"])
B1 = np.array(PESOS["capa_oculta"]["sesgos"])
W2 = np.array(PESOS["capa_salida"]["pesos"])
B2 = np.array(PESOS["capa_salida"]["sesgos"])

# Rango permitido para cada dato que llega a la API
LIMITES = {
    "dias_restantes": (0, 365),
    "horas_planeadas": (0, 24),
    "prioridad": (0, 2),
    "tareas_pendientes": (1, 100),
}

app = Flask(__name__)


def calcular_probabilidad(datos):
    entrada = np.array([min(datos[v] / ESCALAS[v], 1.0) for v in VARIABLES])
    oculta = np.maximum(0, entrada @ W1 + B1)          # capa oculta con ReLU
    z = oculta @ W2 + B2                                # capa de salida
    return float(1 / (1 + np.exp(-z[0])))               # Sigmoid


def validar(datos):
    if not isinstance(datos, dict):
        return "El cuerpo de la solicitud debe ser un JSON con los datos de la tarea."
    for campo, (minimo, maximo) in LIMITES.items():
        valor = datos.get(campo)
        if valor is None:
            return f"Falta el dato '{campo}'."
        if isinstance(valor, bool) or not isinstance(valor, (int, float)):
            return f"El dato '{campo}' debe ser un número."
        if valor < minimo or valor > maximo:
            return f"El dato '{campo}' debe estar entre {minimo} y {maximo}."
    return None


def clasificar(probabilidad):
    if probabilidad >= 0.70:
        return "bajo", "Vas bien de tiempo. Mantén tu ritmo y no la dejes para el último día."
    if probabilidad >= 0.40:
        return "medio", "Hay riesgo de retraso. Conviene dedicarle más horas o empezar pronto."
    return "alto", "Riesgo alto de entregar tarde. Prioriza esta tarea y reserva tiempo cuanto antes."


@app.get("/salud")
def salud():
    return jsonify({"estado": "ok"})


@app.post("/predecir")
def predecir():
    datos = request.get_json(silent=True)
    error = validar(datos)
    if error:
        return jsonify({"error": error}), 400

    probabilidad = calcular_probabilidad(datos)
    riesgo, recomendacion = clasificar(probabilidad)
    return jsonify({
        "probabilidad": round(probabilidad, 4),
        "riesgo": riesgo,
        "recomendacion": recomendacion,
    })


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
