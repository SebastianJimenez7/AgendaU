"""
Red neuronal de AgendaU: probabilidad de entregar una tarea a tiempo.

Entradas (4 variables):
  1. dias_restantes     -> días que faltan para la entrega
  2. horas_planeadas    -> horas que el estudiante planea dedicar a la tarea
  3. prioridad          -> 0 baja, 1 media, 2 alta
  4. tareas_pendientes  -> cantidad de tareas pendientes al mismo tiempo
Salida:
  Probabilidad (0 a 1) de entregar la tarea a tiempo.

Arquitectura: 4 entradas -> 8 neuronas ocultas (ReLU) -> 1 neurona de salida (Sigmoid)

NOTA: AgendaU todavía no tiene un historial real de entregas, así que los datos
de entrenamiento se generan de forma simulada (con una regla y algo de azar).
Son datos didácticos. Con un historial real, bastaría reemplazar el CSV.
"""
import csv
import json
import os

import numpy as np
from tensorflow import keras
from tensorflow.keras import layers

SEMILLA = 42
keras.utils.set_random_seed(SEMILLA)

CARPETA = os.path.dirname(os.path.abspath(__file__))
RUTA_DATOS = os.path.join(CARPETA, "datos", "entregas_simuladas.csv")
RUTA_MODELO = os.path.join(CARPETA, "modelo", "modelo_entrega.keras")
RUTA_PESOS = os.path.join(CARPETA, "modelo", "pesos.json")
RUTA_RESULTADOS = os.path.join(CARPETA, "evidencias", "resultados.txt")
RUTA_GRAFICA = os.path.join(CARPETA, "evidencias", "entrenamiento.png")

VARIABLES = ["dias_restantes", "horas_planeadas", "prioridad", "tareas_pendientes"]
# Valor máximo de cada variable; se usa para llevar todas las entradas a la escala 0 - 1
ESCALAS = {"dias_restantes": 14, "horas_planeadas": 10, "prioridad": 2, "tareas_pendientes": 10}


def generar_datos(cantidad=600):
    azar = np.random.default_rng(SEMILLA)
    dias = azar.integers(0, 15, cantidad)
    horas = azar.uniform(0, 10, cantidad).round(1)
    prioridad = azar.integers(0, 3, cantidad)
    tareas = azar.integers(1, 11, cantidad)

    # Más días y más horas ayudan a entregar a tiempo; más tareas juntas lo dificultan
    z = -2.0 + 0.25 * dias + 0.45 * horas - 0.30 * tareas + 0.25 * prioridad
    probabilidad = 1 / (1 + np.exp(-z))
    a_tiempo = (azar.random(cantidad) < probabilidad).astype(int)
    return np.column_stack([dias, horas, prioridad, tareas]), a_tiempo


def guardar_csv(X, y):
    os.makedirs(os.path.dirname(RUTA_DATOS), exist_ok=True)
    with open(RUTA_DATOS, "w", newline="", encoding="utf-8") as archivo:
        escritor = csv.writer(archivo)
        escritor.writerow(VARIABLES + ["entrega_a_tiempo"])
        for fila, etiqueta in zip(X, y):
            escritor.writerow(list(fila) + [int(etiqueta)])


def normalizar(X):
    escalas = np.array([ESCALAS[v] for v in VARIABLES], dtype=float)
    return np.clip(X / escalas, 0, 1)


def calcular_con_numpy(X_normalizado, pesos):
    # Mismo cálculo que hace la red, pero paso a paso (así lo usa la API)
    oculta = np.maximum(0, X_normalizado @ pesos["capa_oculta"]["pesos"] + pesos["capa_oculta"]["sesgos"])
    z = oculta @ pesos["capa_salida"]["pesos"] + pesos["capa_salida"]["sesgos"]
    return 1 / (1 + np.exp(-z))


def main():
    print("1. Preparando los datos...")
    X, y = generar_datos()
    guardar_csv(X, y)
    X_norm = normalizar(X)

    orden = np.random.default_rng(SEMILLA).permutation(len(X))
    corte = int(len(X) * 0.8)
    X_train, X_test = X_norm[orden[:corte]], X_norm[orden[corte:]]
    y_train, y_test = y[orden[:corte]], y[orden[corte:]]
    print(f"   Datos de entrenamiento: {len(X_train)} | Datos de prueba: {len(X_test)}")

    print("2. Creando la red neuronal (4 -> 8 -> 1)...")
    modelo = keras.Sequential([
        keras.Input(shape=(4,)),
        layers.Dense(8, activation="relu"),
        layers.Dense(1, activation="sigmoid"),
    ])
    modelo.compile(optimizer="adam", loss="binary_crossentropy", metrics=["accuracy"])
    modelo.summary()

    print("3. Entrenando (150 épocas)...")
    historial = modelo.fit(X_train, y_train, epochs=150, batch_size=16,
                           validation_data=(X_test, y_test), verbose=0)

    perdida, precision = modelo.evaluate(X_test, y_test, verbose=0)
    precision_train = historial.history["accuracy"][-1]
    print(f"   Precisión en entrenamiento: {precision_train:.2%}")
    print(f"   Precisión en prueba:        {precision:.2%}")

    print("4. Probando diferentes entradas...")
    casos = [
        ("Poco tiempo, pocas horas, muchas tareas", [1, 1, 1, 8]),
        ("Tiempo medio, horas medias", [5, 4, 1, 5]),
        ("Buen margen y buena dedicación", [10, 7, 1, 3]),
        ("Prioridad alta con tiempo y dedicación", [12, 8, 2, 2]),
    ]
    lineas = ["Resultados de la red neuronal de AgendaU", "",
              f"Precisión en entrenamiento: {precision_train:.2%}",
              f"Precisión en prueba: {precision:.2%}", "",
              "dias  horas  prioridad  tareas  ->  probabilidad de entregar a tiempo"]
    for nombre, valores in casos:
        entrada = normalizar(np.array([valores], dtype=float))
        p = float(modelo.predict(entrada, verbose=0)[0][0])
        linea = f"{valores[0]:>4}  {valores[1]:>5}  {valores[2]:>9}  {valores[3]:>6}  ->  {p:.2f} ({p:.0%})   {nombre}"
        lineas.append(linea)
        print("   " + linea)

    print("5. Guardando el modelo...")
    os.makedirs(os.path.dirname(RUTA_MODELO), exist_ok=True)
    modelo.save(RUTA_MODELO)

    capa1, capa2 = modelo.layers[0], modelo.layers[1]
    w1, b1 = capa1.get_weights()
    w2, b2 = capa2.get_weights()
    pesos = {
        "variables": VARIABLES,
        "escalas": ESCALAS,
        "capa_oculta": {"pesos": w1.tolist(), "sesgos": b1.tolist()},
        "capa_salida": {"pesos": w2.tolist(), "sesgos": b2.tolist()},
    }
    with open(RUTA_PESOS, "w", encoding="utf-8") as archivo:
        json.dump(pesos, archivo)

    # Verificación: el cálculo con NumPy debe dar lo mismo que Keras
    esperado = modelo.predict(X_test, verbose=0)
    calculado = calcular_con_numpy(X_test, {
        "capa_oculta": {"pesos": w1, "sesgos": b1},
        "capa_salida": {"pesos": w2, "sesgos": b2},
    })
    diferencia = float(np.max(np.abs(esperado - calculado)))
    lineas += ["", f"Diferencia máxima entre Keras y el cálculo manual: {diferencia:.8f}"]
    print(f"   Diferencia máxima Keras vs cálculo manual: {diferencia:.8f}")

    os.makedirs(os.path.dirname(RUTA_RESULTADOS), exist_ok=True)
    with open(RUTA_RESULTADOS, "w", encoding="utf-8") as archivo:
        archivo.write("\n".join(lineas) + "\n")

    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        figura, ejes = plt.subplots(1, 2, figsize=(10, 4))
        ejes[0].plot(historial.history["loss"], label="Entrenamiento")
        ejes[0].plot(historial.history["val_loss"], label="Prueba")
        ejes[0].set_title("Pérdida")
        ejes[0].set_xlabel("Época")
        ejes[0].legend()
        ejes[1].plot(historial.history["accuracy"], label="Entrenamiento")
        ejes[1].plot(historial.history["val_accuracy"], label="Prueba")
        ejes[1].set_title("Precisión")
        ejes[1].set_xlabel("Época")
        ejes[1].legend()
        figura.tight_layout()
        figura.savefig(RUTA_GRAFICA, dpi=120)
        print("   Gráfica guardada en evidencias/entrenamiento.png")
    except ImportError:
        print("   (matplotlib no está instalado; se omite la gráfica)")

    print("Listo. Ahora puedes ejecutar: python api.py")


if __name__ == "__main__":
    main()
