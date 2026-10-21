# CyberMecha: Roguelike Brick Breaker

Transformación de la experiencia arcade tradicional en un **juego de combate mecha cyberpunk estilo roguelike**, donde controlas una plataforma acorazada de asalto pilotada por acelerómetro para erradicar **núcleos de IA corrompidos (jefes finales)**, desbloquear **reliquias tácticas** entre sectores y activar **cañones de plasma frontales** y un **escudo de energía con enfriamiento**.

---

### Decisiones Críticas y Resumen de Preferencias del Usuario

> [!IMPORTANT]
> Decisiones confirmadas a través del proceso de aclaración:
> - **Avatar / Paleta:** Mecha acorazado cyberpunk con alas vectoriales, blindaje reforzado, cañones dobles y reactor de pulso central.
> - **Enemigos y Jefes:** Inteligencias artificiales corrompidas al final de cada nivel con barra de vida segmentada, patrones de disparo y escudos reactivos.
> - **Progresión Roguelike:** Pantalla de selección táctica de 1 de 3 reliquias / aumentos cibernéticos aleatorios tras derrotar a cada jefe o fase.
> - **Habilidades Activas:** Botones táctiles ergonómicos con tiempo de recarga (*cooldown*) para:
>   1. **Cañones de Plasma Frontales:** Dispara ráfagas de proyectiles ascendentes que dañan bloques y jefes.
>   2. **Escudo de Energía Cuántica:** Despliega una barrera temporal bajo el mecha que rebota cualquier bola perdida.

---

## 1. Visión General y Concepto Central

- **Concepto:** En el año 2184, una red de IAs neurales autónomas ha tomado los servidores centrales de la megaciudad. Como piloto del mecha de asalto *Aegis-01*, debes atravesar cuadrículas de cortafuegos de neón y destruir las matrices de los megajefes IA.
- **Ciclo de Juego Roguelike (*Core Loop*):**
  1. **Fase de Brecha (Ladrillos de Firewall):** Rompe la defensa de ladrillos usando la bola de energía cinética guiada por la inclinación de tu dispositivo móvil.
  2. **Fase de Duelo de Jefe (*Boss Encounter*):** Al limpiar la barrera frontal, desciende el avatar de la IA corrupta (con ataques de misiles, rayos o minas que debes esquivar o repeler).
  3. **Selección de Aumento Cibernético (1 de 3 Reliquias):** Escoge mejoras como *Balas Penetrantes*, *Recarga Acelerada de Cañones*, *Blindaje Nanotecnológico*, *Doble Esfera de Fusión*, o *Láser Fotónico*.
  4. **Escalada de Dificultad:** Cada sector presenta IAs con patrones de ataque más agresivos y mayor durabilidad.

---

## 2. Experiencia de Usuario y Diseño Visual

### 2.1 Identidad Visual y Atmósfera Cyberpunk
- **Paleta de Colores Neón Industrial:**
  - Fondo: Vacío de silicio y rejilla ciberespacial profunda (`#0A071B` y `#120A2E`).
  - Mecha Jugador: Titanio oscuro con iluminación cian eléctrica (`#00F0FF`) y propulsores naranja fuego (`#FF6600`).
  - Jefes IA: Rojo carmesí de alerta (`#FF0055`), violeta cibernético (`#A855F7`) y amarillo radiación (`#FFE600`).
- **Aspecto Épico del Mecha (Sustituto de la Paleta):**
  - Chasis estilizado con alerones angulares, toberas de propulsión con partículas y dos cañones montados en los extremos.
  - Indicador visual del escudo activo mediante un domo traslúcido hexagonal.

### 2.2 Flujo y Pantallas de Usuario
1. **HUD de Combate Táctico:**
   - Barra de vida del jefe con nombre en código (p. ej., *NEXUS-V*, *OVERLORD-KRNL*, *ZERO-NEURAL*).
   - Indicador de blindaje (escudos/vidas del mecha) e indicador de inclinación del acelerómetro.
   - Dos botones flotantes de acción rápida en las esquinas inferiores con círculos de enfriamiento (*cooldown radial*):
     - **Botón Izquierdo:** *Escudo Deflector* (tecla de salvación con 20s de recarga).
     - **Botón Derecho:** *Fuego de Cañones* (ráfaga de 6 disparos con 6s de recarga).
2. **Encuentros con Jefes:**
   - Animación de entrada de alarma cibernética (*WARNING: CORRUPTED CORE DETECTED*).
   - El jefe se desplaza horizontalmente disparando orbes corruptos hacia abajo que el mecha puede esquivar o absorber con escudo.
3. **Selector de Reliquias (Fin de Nivel):**
   - Modal estilo terminal cibernética con 3 tarjetas holográficas animadas (icono, nombre de reliquia, descripción de ventaja pasiva y rareza: Común, Rara, Legendaria).

---

## 3. Decisiones de Producto y Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                       ArcanoidScreen                        │
├──────────────────────────────┬──────────────────────────────┤
│        ArcanoidHud           │      BossHealthBar & HUD     │
│   (Vidas, Nivel, Reliquias)  │   (Vida IA, Alarma Boss)     │
├──────────────────────────────┴──────────────────────────────┤
│                       ArcanoidCanvas                        │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  - Jefe IA (Movimiento, Estados de Ataque, Hitbox)  │   │
│   │  - Ladrillos Cortafuegos / Bloques Blindados        │   │
│   │  - Proyectiles de Cañones del Mecha                 │   │
│   │  - Proyectiles Hostiles del Jefe                    │   │
│   │  - Mecha Acorazado (Alerones, Reactores, Cañones)   │   │
│   │  - Bola de Energía Cinética y Estelas Neón          │   │
│   │  - Partículas y Escudo Hexagonal                    │   │
│   └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│         Botones de Habilidad Activa (M3 Floating HUD)        │
│    [ 🛡️ Escudo Cuántico ]          [ ⚡ Disparar Cañones ]    │
└─────────────────────────────────────────────────────────────┘
                               ▲
                               │
               ┌───────────────┴───────────────┐
               │        ArcanoidEngine         │
               │  - Ciclo de Física 60/120 FPS │
               │  - Lógica de Jefes & Ataques  │
               │  - Pool de Reliquias & Stats  │
               │  - Recargas de Habilidades    │
               │  - Sensor Acelerómetro Directo│
               └───────────────────────────────┘
```

### 3.1 Clases y Entidades de Datos
- **`Boss`**: Entidad con posición, tamaño, puntos de vida (`currentHp`, `maxHp`), fase de ataque, temporizador de proyectiles hostiles y sprite vectorial procedimental único por jefe.
- **`MechProjectile`**: Proyectiles de plasma disparados por los cañones del jugador que suben verticalmente dañando ladrillos y al jefe.
- **`EnemyProjectile`**: Proyectiles lanzados por los jefes que bajan hacia el mecha; si impactan al jugador sin escudo, reducen un escudo/vida.
- **`Relic`**: Sistema de reliquias roguelike (p. ej., *Balas Perforantes*, *Doble Disparo de Cañón*, *Paleta Acorazada +30%*, *Enfriamiento -25%*, *Imán de Power-ups*, *Rebote Eléctrico*).
- **`ActiveSkillState`**: Control de recarga y duración para Cañones (disparo con ráfagas) y Escudo Temporal.

---

## 4. Plan de Verificación y Pruebas

- **Física y Control:** Comprobar que el control por acelerómetro mantiene la respuesta ultra fluida sin saltos mientras se usan los botones de disparo y escudo con los pulgares.
- **Jefes Progresivos:** Verificar que el jefe del Nivel 1 introduce mecánicas simples, mientras que los niveles 2 y 3 incorporan patrones múltiples de disparo y escudos orbitales.
- **Compilación y Tests:** Ejecutar `compile_applet` y los tests unitarios en Robolectric comprobando las colisiones con el jefe, el disparo de cañones y la aplicación de reliquias.
