# OpenGL Java Class (LWJGL)

Proyecto base de OpenGL en Java usando **LWJGL + GLFW**, con dos entradas:

- `com.graphics.App` (triángulo básico)
- `com.graphics.AppMovimientoTeclado` (triángulo movible con teclado)

## Requisitos

- Java 17 o superior
- Maven 3.9+
- macOS (este `pom.xml` ya incluye `natives-macos` y `natives-macos-arm64`)

## 1) Crear un proyecto Maven (desde cero)

Si quieres crear un proyecto nuevo igual a este formato:

```bash
mvn archetype:generate \
  -DgroupId=com.graphics \
  -DartifactId=opengl-java-class \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

Luego entra al proyecto:

```bash
cd opengl-java-class
```

Después debes:

1. Reemplazar el `pom.xml` por uno con dependencias de LWJGL/GLFW/OpenGL.
2. Crear las clases en `src/main/java/com/graphics/`.

## 2) Ubicarte en este proyecto

En este repo en particular, la carpeta que contiene el `pom.xml` es:

```bash
cd "/Users/kenjikv/Documents/Personal/Personal/ProgramacionGrafica/OpenGL/Clase 01/opengl-java-class/opengl-java-class"
```

## 3) Compilar

```bash
mvn compile
```

## 4) Ejecutar cada app por separado

### Ejecutar `App` (triángulo base)

```bash
mvn compile exec:exec -DmainClass=com.graphics.App
```

Tambien puedes ejecutarla con la clase por defecto definida en `pom.xml`:

```bash
mvn exec:exec
```

### Ejecutar `AppMovimientoTeclado` (mover con WASD/flechas)

```bash
mvn compile exec:exec -DmainClass=com.graphics.AppMovimientoTeclado
```

## Controles en `AppMovimientoTeclado`

- `W` / `Flecha Arriba`: mover arriba
- `S` / `Flecha Abajo`: mover abajo
- `A` / `Flecha Izquierda`: mover izquierda
- `D` / `Flecha Derecha`: mover derecha
- `ESC`: cerrar ventana

## Problema comun: "no encuentra POM"

Si ves un error de Maven indicando que no hay `pom.xml`, estas ejecutando en la carpeta incorrecta.
Debes ejecutar los comandos dentro de:

`.../opengl-java-class/opengl-java-class`




## ================================================================================================
## ++++++++++++++++++++++++++++++++++++++++MODIFICACIONES++++++++++++++++++++++++++++++++++++++++++
## ================================================================================================


# Modificaciones principales realizadas

La versión modificada del proyecto amplía significativamente el juego original tanto a nivel gráfico como estructural. Las mejoras más importantes fueron las siguientes:

---

## Sistema multijugador

El juego pasó de tener un solo jugador a soportar **dos pájaros simultáneamente**.

Cada jugador posee:

- física independiente,
- colisiones propias,
- puntaje separado,
- controles distintos.

### Controles

- `SPACE` → Jugador 1
- `W` → Jugador 2

---

## Nueva clase `Bird`

El pájaro dejó de ser un simple rectángulo y ahora se construye utilizando múltiples figuras geométricas.

| Parte | Figura utilizada |
|---|---|
| Cuerpo | Rectángulo |
| Ojos | Círculos |
| Pico | Triángulo |
| Alas | Triángulos rotados |
| Cola | Rectángulo |

Esto permitió crear un personaje mucho más detallado visualmente.

---

## Sistema de rotación en shaders

Se agregó el uniform:

```glsl
uniform float uRotation;
```

Con esto ahora es posible:

- rotar triángulos,
- inclinar alas,
- orientar figuras,
- reutilizar geometría base.

---

## Nuevas geometrías base

El proyecto originalmente solo utilizaba un `quad`.

Ahora se añadieron:

- `Quad` → rectángulos,
- `Círculo` → ojos,
- `Triángulo` → alas, montañas y pico.

---

## Fondo con capas visuales

Se implementó un sistema de render por capas:

1. Cielo
2. Nubes
3. Montañas
4. Tuberías
5. Pájaros
6. HUD
7. Overlay de Game Over

Esto genera sensación de profundidad y mejora visual.

---

## Clase `Montanas`

Se agregó un fondo de montañas creado completamente con triángulos.

Cada montaña posee:

- cuerpo principal,
- cara iluminada,
- nieve en la punta.

Además, las montañas se mueven con efecto parallax.

---

## Clase `Hud`

Se creó un HUD personalizado para mostrar:

- barra superior,
- paneles de jugadores,
- puntajes digitales.

Los números se renderizan manualmente usando rectángulos estilo display digital de 7 segmentos.

---

## Soporte para texturas

El proyecto ahora utiliza imágenes mediante `STBImage`.

Se añadieron texturas para:

- nubes,
- pantalla de Game Over.

---

## Overlay de Game Over

El Game Over dejó de ser una franja simple y ahora utiliza:

- una imagen renderizada,
- transparencia (`GL_BLEND`),
- texturas OpenGL.

---

## Mejora visual de tuberías

Las tuberías fueron rediseñadas agregando:

- iluminación lateral,
- sombras,
- bordes superiores,
- distintos tonos de verde.

Esto les da una apariencia menos plana.

---

## Dificultad progresiva

La velocidad de las tuberías ahora aumenta conforme avanza la partida:

```java
VELOCIDAD_TUBERIAS =
    0.62f + (puntaje / 10) * 0.15f;
```

Generando una dificultad dinámica.

---

## Modularización del proyecto

El código fue reorganizado en múltiples clases:

| Clase | Función |
|---|---|
| `Bird` | lógica y render del jugador |
| `Hud` | puntajes |
| `Montanas` | fondo |
| `Nubes` | texturas de nubes |
| `AppFlappyBird` | flujo principal |

Esto mejora la organización y mantenimiento del proyecto.


## ==========================================================================================================
## ==========================================================================================================

## Clase `Nubes`

La clase `Nubes` administra el sistema de nubes decorativas del juego. Su función es mejorar el escenario visual generando sensación de movimiento y profundidad.

### Funciones principales

- Cargar texturas PNG usando `STBImage`.
- Crear y almacenar nubes dinámicamente.
- Mover las nubes lentamente para efecto parallax.
- Generar nuevas nubes aleatorias automáticamente.
- Eliminar nubes que salen de pantalla.
- Dibujar las nubes utilizando OpenGL y transparencias.

### Características

- Uso de texturas con OpenGL.
- Movimiento dinámico del fondo.
- Generación procedural de nubes.
- Efecto visual de profundidad.
- Sistema optimizado mediante listas dinámicas.

La clase contribuye a que el escenario del juego sea más dinámico y visualmente atractivo.

## ==========================================================================================================
## ==========================================================================================================

## Clase `Bird`

La clase `Bird` representa a un jugador dentro del juego y encapsula toda la lógica relacionada con el pájaro.

### Funciones principales

- Manejo de posición y movimiento vertical.
- Sistema de física con gravedad y salto.
- Detección de colisiones con tuberías y límites de pantalla.
- Control de estado de vida (`vivo`).
- Reinicio del jugador al comenzar una nueva partida.
- Renderizado completo del pájaro usando figuras OpenGL.

### Características

- Física basada en gravedad y velocidad vertical.
- Sistema de muerte al colisionar.
- Renderizado modular mediante:
  - Rectángulos
  - Círculos
  - Triángulos
- Uso de shaders y uniforms OpenGL.
- Compatible con modo de 1 y 2 jugadores.

El pájaro se dibuja manualmente utilizando primitivas geométricas, incluyendo cuerpo, ojos, pico, alas y cola.

## ==========================================================================================================
## ==========================================================================================================

## Clase `Hud`

La clase `Hud` se encarga de dibujar la interfaz gráfica del juego (HUD), mostrando el puntaje de los jugadores en pantalla.

### Funciones principales

- Dibujar la barra superior del juego.
- Mostrar el puntaje del jugador 1.
- Mostrar el puntaje del jugador 2.
- Actualizar los números en tiempo real.
- Renderizar números estilo display digital.

### Características

- HUD completamente dibujado con OpenGL.
- Uso de rectángulos para formar números digitales.
- Sistema dinámico de actualización de puntajes.
- Compatible con modo multijugador.
- Diseño integrado dentro de la ventana del juego.

La clase mejora la interfaz visual mostrando información importante directamente dentro de la partida.

## ==========================================================================================================
## ==========================================================================================================

### Clase `Montanas`

La clase `Montanas` se encarga de generar y renderizar el paisaje montañoso del escenario utilizando figuras geométricas básicas en OpenGL. Todas las montañas están construidas completamente con triángulos renderizados mediante shaders y VAOs.

Cada montaña se compone de 3 triángulos:
- Un triángulo principal grande que representa el cuerpo de la montaña.
- Un segundo triángulo ligeramente desplazado y de color más claro para simular iluminación y profundidad visual.
- Un tercer triángulo pequeño ubicado en la punta que representa nieve.

La clase implementa dos capas distintas de montañas:
- Una fila trasera con montañas más pequeñas y lentas.
- Una fila delantera con montañas más grandes y rápidas.

Esto permite crear un efecto visual tipo *parallax*, donde los objetos cercanos se desplazan más rápido que los lejanos, generando mayor sensación de profundidad en el escenario.

Además, las montañas se desplazan continuamente hacia la izquierda junto con el escenario y se reciclan automáticamente al salir de pantalla, permitiendo crear un fondo infinito sin necesidad de generar nuevas estructuras constantemente.


## ==========================================================================================================
## ==========================================================================================================

# Menú Principal — Flappy Bird

La clase `Menu` implementa la pantalla principal del juego utilizando LWJGL y OpenGL.

El sistema renderiza completamente la interfaz de manera manual mediante primitivas gráficas básicas como rectángulos, círculos y triángulos, sin utilizar motores gráficos externos ni librerías de UI.

## Características

- Pantalla inicial interactiva.
- Selección de:
  - `1 Jugador`
  - `2 Jugadores`
- Navegación mediante teclado:
  - `↑` y `↓` para cambiar opción
  - `SPACE` y `ENTER` para confirmar
  - `W` como acceso rápido al modo multijugador
- Animación flotante de pájaros decorativos.
- Sistema personalizado de texto digital estilo arcade.
- Renderizado manual usando OpenGL.

---

# Sistema de Renderizado

El menú está construido usando primitivas OpenGL:

| Elemento | Método |
|---|---|
| Panel principal | `dibujarRect()` |
| Texto digital | `dibujarLetras()` |
| Letras especiales | `dibujarLetraEspecial()` |
| Pájaros decorativos | `dibujarPajaroDecorativo()` |
| Círculos | `dibujarCirculo()` |
| Triángulos | `dibujarTriangulo()` |

---

# Sistema de Letras

El texto del menú utiliza un sistema de caracteres tipo “7 segmentos”, donde cada letra se forma activando combinaciones de segmentos rectangulares.

Esto permite renderizar texto completamente con OpenGL sin usar fuentes externas.

También se implementaron letras especiales (`b`, `r`, `d`, `W`) para mejorar la apariencia visual del título y controles.

---

# Animaciones

Los pájaros decorativos utilizan movimiento senoidal:

```java
Math.sin(tiempoAnimacion * 2.5f)






# 🎮 Controles del Juego

El juego cuenta con dos modos seleccionables desde el menú principal:

- **1 Jugador**
- **2 Jugadores**

La selección se realiza directamente desde el menú interactivo del juego.

---

# 🕹️ Controles del Menú

| Tecla | Acción |
|---|---|
| ⬆️ / ⬇️ | Navegar entre opciones |
| `SPACE` | Seleccionar opción actual |
| `W` | Atajo directo para iniciar modo 2 jugadores |
| `ENTER` | Confirmar opción seleccionada |

---

# 👤 Modo 1 Jugador

En este modo, un solo jugador controla al pájaro.

| Tecla | Acción |
|---|---|
| `SPACE` | Hacer saltar al pájaro |

---

# 👥 Modo 2 Jugadores

En el modo multijugador, cada jugador tiene su propia tecla de control.

| Jugador | Tecla | Acción |
|---|---|---|
| Jugador 1 | `SPACE` | Saltar |
| Jugador 2 | `W` | Saltar |

---

# Reinicio y Regreso al Menú

Cuando la partida termina:

| Tecla | Acción |
|---|---|
| `SPACE` | Reiniciar la partida |
| `R` | Volver al menú principal |

---

# Características del Menú

El menú principal incluye:

- Título digital estilo retro **FLAPPY BIRD**
- Sistema visual de selección
- Animaciones de pájaros decorativos
- Indicadores de opción activa
- Navegación con teclado
- Soporte para 1 y 2 jugadores
- Renderizado completamente con OpenGL y LWJGL

---