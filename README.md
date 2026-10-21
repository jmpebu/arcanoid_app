# Neon Arcanoid: CyberMecha Roguelike

**Neon Arcanoid: CyberMecha** es un juego arcade de romper ladrillos estilo *Arkanoid / Breakout* reinventado con mecánicas **roguelike**, estética **retro-cyberpunk** de colores de neón vistosos y control mediante el **acelerómetro** del dispositivo móvil.

---

## 🎮 Características Principales

### 📱 Control por Acelerómetro Ultra Fluido
- **Mapeo Directo por Inclinación:** La posición del mecha se calcula suavemente a partir de la inclinación del teléfono con amortiguación exponencial (*smooth damping*), eliminando derivas y saltos bruscos.
- **Nivel de Burbuja en Tiempo Real:** Visualización en el HUD con los grados exactos de inclinación del dispositivo.
- **Calibración Rápida:** Botón *"Calibrar"* para fijar la postura neutra de juego (sentado, tumbado, etc.).
- **Modo Táctil / Híbrido:** Posibilidad de jugar arrastrando el dedo por la pantalla en emuladores o superficies planas.

### 🤖 Mecha Acorazado de Combate
- Sustituye a la clásica barra plana por un caza mecha cyberpunk con alerones angulares en titanio oscuro y neón cian.
- Cañones de plasma dobles en los extremos de las alas, reactor de fusión central y toberas de propulsión con partículas de fuego.

### 👾 Jefes Finales: Inteligencias Artificiales Corrompidas
Cada sector está custodiado por una supercomputadora IA con barra de vida reactiva en tiempo real y patrones de ataque propios:
- **Sector 1 — NEXUS-V:** Núcleo corrupto del firewall que dispara orbes de energía dirigidos.
- **Sector 2 — OVERLORD-KRNL:** Centinela cuántico autónomo con fuego doble en abanico.
- **Sector 3 — ZERO-NEURAL:** Singularidad rogue de alta cadencia y resistencia extrema.
- **Sectores Superiores:** Escala procedimentalmente en puntos de vida y agresividad.

### ⚡ Habilidades Activas para Pulgares
- **Cañones de Plasma (⚡, esquina inferior derecha):** Dispara una ráfaga de proyectiles de energía ascendentes que perforan ladrillos y dañan directamente a los jefes.
- **Escudo Cuántico (🛡️, esquina inferior izquierda):** Proyecta un domo de energía hexagonal que desvía proyectiles hostiles y activa un suelo láser temporal para salvar bolas en caída libre.

### 🧬 Progresión Roguelike (1 de 3 Reliquias)
Al erradicar al jefe de cada sector, accedes a la terminal de aumentos para elegir 1 de 3 reliquias cibernéticas aleatorias:
- **Cañones Cuádruples:** Duplica los cañones del mecha disparando 4 proyectiles simultáneos.
- **Refrigeración Cuántica:** Reduce un 35% el tiempo de recarga de todas las habilidades activas.
- **Nanobots Regenerativos:** Concede +1 vida máxima y repara el blindaje al instante.
- **Fusión de Plasma:** La bola de energía causa un +100% de daño a jefes y destruye ladrillos duros en 1 golpe.
- **Alas de Asalto:** Expande permanentemente el ancho del mecha un +20%.
- **Escudo Reflector:** Prolonga la duración de la barrera cuántica y repele el fuego enemigo.

---

## 🛠️ Arquitectura Técnica

- **Lenguaje:** Kotlin 100%.
- **Interfaz de Usuario:** Jetpack Compose (Material Design 3) con lienzo `Canvas` optimizado.
- **Rendimiento Gráfico:** Bucle de física a 60/120 FPS desacoplado de la recomposición de Compose mediante `renderTrigger` en `DrawScope`, eliminando pausas de *Garbage Collector* con preasignación de trazados y pinturas.
- **Sensores:** `SensorManager` escuchando `Sensor.TYPE_ACCELEROMETER` con filtrado pasabajo (*exponential moving average*).
- **Retroalimentación Háptica y Sonido:** Android `Vibrator` y `ToneGenerator` con limitación de frecuencia (*throttling*) para efectos táctiles y sonoros arcade.
- **Persistencia:** `SharedPreferences` para el guardado local del récord personal (*High Score*).

---

## 📂 Estructura del Código Fuente

```
app/src/main/java/com/example/
├── MainActivity.kt               # Actividad principal con edge-to-edge y FLAG_KEEP_SCREEN_ON
├── game/
│   ├── audio/
│   │   └── HapticSoundHelper.kt  # Gestión de vibraciones y sintetizador de tonos arcade
│   ├── engine/
│   │   └── ArcanoidEngine.kt     # Motor de física, colisiones, jefes, reliquias y bucle de juego
│   ├── level/
│   │   └── LevelGenerator.kt     # Generación de niveles de ladrillos y jefes IA
│   ├── model/
│   │   └── GameModels.kt         # Modelos de datos (Boss, Relic, Brick, Ball, Projectiles)
│   ├── sensor/
│   │   └── AccelerometerController.kt # Controlador de acelerómetro, calibración y nivel de burbuja
│   └── ui/
│       ├── ArcanoidCanvas.kt     # Renderizado de mecha, jefes, estelas, proyectiles y partículas
│       ├── ArcanoidDialogs.kt    # Modales de selección de reliquias, fin de partida y ajustes
│       ├── ArcanoidHud.kt        # Marcadores de vida, puntuación, barra del jefe y burbuja
│       └── ArcanoidScreen.kt     # Composable raíz con bucle VSYNC y botones de habilidades
└── ui/theme/
    ├── Color.kt                  # Paleta neón (Cian, Rosa, Púrpura, Verde, Amarillo, Oro, Carmesí)
    └── Theme.kt                  # Tema oscuro cyberpunk con Material 3
```

---

## 🚀 Compilación y Pruebas

Para compilar el proyecto o ejecutar los tests unitarios en el entorno:

```bash
# Compilar el APK de depuración
gradle :app:assembleDebug

# Ejecutar las pruebas unitarias locales (Robolectric)
gradle :app:testDebugUnitTest
```

---

## 📜 Licencia
Proyecto desarrollado en Google AI Studio Build.
