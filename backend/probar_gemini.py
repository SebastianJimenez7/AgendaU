"""
Prueba rápida de la clave y el modelo de Gemini, sin usar la app.

Uso (desde la carpeta backend):
    python probar_gemini.py
    python probar_gemini.py nombre-del-modelo

Lee GEMINI_API_KEY y GEMINI_MODEL del archivo frontend/local.properties.
No muestra la clave en pantalla.
"""
import json
import os
import sys
import urllib.error
import urllib.request

CARPETA = os.path.dirname(os.path.abspath(__file__))
RUTA = os.path.join(CARPETA, "..", "frontend", "local.properties")
BASE = "https://generativelanguage.googleapis.com/v1beta/"


def leer_propiedades():
    propiedades = {}
    if os.path.exists(RUTA):
        with open(RUTA, encoding="utf-8") as archivo:
            for linea in archivo:
                linea = linea.strip()
                if linea and not linea.startswith("#") and "=" in linea:
                    nombre, valor = linea.split("=", 1)
                    propiedades[nombre.strip()] = valor.strip()
    return propiedades


def llamar(url, clave, cuerpo=None):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo else None
    solicitud = urllib.request.Request(url, data=datos, method="POST" if cuerpo else "GET")
    solicitud.add_header("x-goog-api-key", clave)
    if cuerpo:
        solicitud.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(solicitud, timeout=60) as respuesta:
        return json.loads(respuesta.read().decode("utf-8"))


def mostrar_error(e):
    try:
        detalle = json.loads(e.read().decode("utf-8"))["error"]["message"]
    except Exception:
        detalle = "(sin detalle)"
    print(f"   ERROR HTTP {e.code}: {detalle[:300]}")


def main():
    propiedades = leer_propiedades()
    clave = propiedades.get("GEMINI_API_KEY", "")
    modelo = sys.argv[1] if len(sys.argv) > 1 else propiedades.get("GEMINI_MODEL", "gemini-flash-latest")

    if not clave:
        print("No se encontró GEMINI_API_KEY en frontend/local.properties")
        return
    print(f"Clave encontrada ({len(clave)} caracteres). Modelo a probar: {modelo}\n")

    print("1. Modelos disponibles con generateContent (los que incluyen 'flash'):")
    try:
        lista = llamar(BASE + "models?pageSize=200", clave).get("models", [])
        nombres = [m["name"].replace("models/", "") for m in lista
                   if "generateContent" in m.get("supportedGenerationMethods", [])]
        for nombre in nombres:
            if "flash" in nombre:
                print("   -", nombre)
    except urllib.error.HTTPError as e:
        mostrar_error(e)
    except Exception as e:
        print("   No se pudo conectar:", e)

    print(f"\n2. Enviando una solicitud de prueba a {modelo}...")
    cuerpo = {"contents": [{"parts": [{"text": "Responde solamente: hola"}]}]}
    try:
        respuesta = llamar(f"{BASE}models/{modelo}:generateContent", clave, cuerpo)
        texto = respuesta["candidates"][0]["content"]["parts"][0]["text"]
        print("   OK. Respuesta del servicio:", texto.strip())
    except urllib.error.HTTPError as e:
        mostrar_error(e)
    except Exception as e:
        print("   No se pudo conectar:", e)


if __name__ == "__main__":
    main()
