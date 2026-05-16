package com.graphics;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

/**
 * AppFlappyBird:
 * Mini-juego estilo Flappy Bird con OpenGL 2D (NDC directo, sin texturas).
 *
 * Flujo de pantallas:
 *   MENU → JUGANDO → GAME OVER → MENU (con R) o reinicio directo (SPACE)
 *
 * Orden de capas (fondo → frente):
 *   1. Fondo azul (glClearColor)
 *   2. Nubes
 *   3. Montañas
 *   4. Tuberías
 *   5. Pájaros
 *   6. HUD de puntaje
 *   7. Overlay game over
 */
public class AppFlappyBird {

    // Tamaño inicial de ventana.
    private static final int ANCHO = 900;
    private static final int ALTO  = 700;

    // Posiciones horizontales fijas de cada pájaro en NDC.
    private static final float BIRD_X  = -0.45f;
    private static final float BIRD2_X = -0.65f;

    // Parámetros de tuberías.
    private static final float TUBERIA_ANCHO         = 0.18f;
    private static final float GAP_ALTO              = 0.48f;
    private float               VELOCIDAD_TUBERIAS   = 0.62f;
    private static final float TIEMPO_ENTRE_TUBERIAS = 1.5f;
    private static final float GAP_MIN_CENTRO        = -0.45f;
    private static final float GAP_MAX_CENTRO        =  0.45f;

    // Recursos OpenGL básicos.
    private long window;
    private int  programa;
    private int  vao;
    private int  vbo;
    private int  vaoCirculo;
    private int  vboCirculo;
    private int  vaoTriangulo;
    private int  vboTriangulo;

    // Uniforms de transformación y color.
    private int uOffsetLocation;
    private int uScaleLocation;
    private int uColorLocation;
    private int uRotationLocation;
    private int uUsarTexturaLocation;

    // Escenario.
    private Nubes    nubes;
    private Montanas montanas;
    private int      textureGameOver;

    // HUD de puntaje
    private Hud hud;

    // *** MENÚ DE INICIO ***
    private Menu   menu;
    private boolean enMenu    = true;   // true = mostrando menú
    private boolean dosJugadores = false; // modo elegido

    // Pájaros.
    private Bird bird1;
    private Bird bird2;

    // Estado del juego.
    private float   timerSpawn;
    private int     puntaje;
    private int     puntaje2;
    private boolean started;
    private boolean gameOver;
    private boolean prevSpace;
    private boolean prevR;
    private boolean prevM;   // para volver al menú con M

    // Lista de obstáculos activos.
    private final List<Tuberia> tuberias = new ArrayList<>();
    private final Random        random   = new Random();

    /** Modelo de una tubería. */
    private static class Tuberia {
        float   x;
        float   gapCentroY;
        boolean puntuada;
        boolean puntuada2;

        Tuberia(float x, float gapCentroY) {
            this.x          = x;
            this.gapCentroY = gapCentroY;
        }
    }

    // ---------------------------------------------------------------
    // Flujo principal
    // ---------------------------------------------------------------

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ---------------------------------------------------------------
    // Inicialización
    // ---------------------------------------------------------------

    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,               GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE,             GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,        GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird OpenGL", 0, 0);
        if (window == 0) throw new RuntimeException("No se pudo crear la ventana");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        crearShaders();
        crearQuadBase();
        crearCirculoBase();
        crearTrianguloBase();

        // Nubes.
        nubes = new Nubes();
        nubes.init("src/main/resources/nube.png");

        // Montañas.
        montanas = new Montanas();
        montanas.init(
            uOffsetLocation,
            uScaleLocation,
            uColorLocation,
            uRotationLocation,
            vaoTriangulo
        );

        // HUD de puntaje.
        hud = new Hud(
            uOffsetLocation,
            uScaleLocation,
            uColorLocation,
            uRotationLocation,
            vao
        );

        // *** MENÚ — se crea una sola vez y se reutiliza ***
        menu = new Menu(
            window,
            uOffsetLocation,
            uScaleLocation,
            uColorLocation,
            uRotationLocation,
            vao,
            vaoCirculo,
            vaoTriangulo
        );

        cargarTexturaGameOver("src/main/resources/gameover.jpg");

        // Pájaros (se crean una vez; reset() los reinicia).
        bird1 = new Bird(
            BIRD_X,  0.0f,
            0.98f, 0.85f, 0.20f,
            uOffsetLocation, uScaleLocation, uColorLocation, uRotationLocation,
            vao, vaoCirculo, vaoTriangulo
        );
        bird2 = new Bird(
            BIRD2_X, 0.25f,
            0.2f, 0.8f, 1.0f,
            uOffsetLocation, uScaleLocation, uColorLocation, uRotationLocation,
            vao, vaoCirculo, vaoTriangulo
        );

        // Arranca en el menú.
        enMenu = true;
        actualizarTituloMenu();
    }

    // ---------------------------------------------------------------
    // Shaders
    // ---------------------------------------------------------------

    private void crearShaders() {
        String vertexSrc = """
            #version 330 core

            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoord;

            uniform vec2  uOffset;
            uniform vec2  uScale;
            uniform float uRotation;
            out vec2 vTexCoord;

            void main() {
                vTexCoord = aTexCoord;
                vec2 scaled = aPos.xy * uScale;
                float cosA = cos(uRotation);
                float sinA = sin(uRotation);
                vec2 rotated = vec2(
                    scaled.x * cosA - scaled.y * sinA,
                    scaled.x * sinA + scaled.y * cosA
                );
                vec2 finalPos = rotated + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        String fragmentSrc = """
            #version 330 core
            in vec2 vTexCoord;
            uniform vec3      uColor;
            uniform sampler2D uTextura;
            uniform bool      uUsarTextura;
            out vec4 fragColor;
            void main() {
                if (uUsarTextura) {
                    fragColor = texture(uTextura, vTexCoord);
                } else {
                    fragColor = vec4(uColor, 1.0);
                }
            }
            """;

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + GL20.glGetProgramInfoLog(programa));
        }

        uOffsetLocation      = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation       = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation       = GL20.glGetUniformLocation(programa, "uColor");
        uRotationLocation    = GL20.glGetUniformLocation(programa, "uRotation");
        uUsarTexturaLocation = GL20.glGetUniformLocation(programa, "uUsarTextura");

        if (uOffsetLocation == -1 || uScaleLocation == -1 || uColorLocation == -1) {
            throw new RuntimeException("No se pudieron obtener uniforms del shader");
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    // ---------------------------------------------------------------
    // Geometría base
    // ---------------------------------------------------------------

    private void crearQuadBase() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,  0.0f, 0.0f,
             0.5f, -0.5f, 0.0f,  1.0f, 0.0f,
             0.5f,  0.5f, 0.0f,  1.0f, 1.0f,
            -0.5f, -0.5f, 0.0f,  0.0f, 0.0f,
             0.5f,  0.5f, 0.0f,  1.0f, 1.0f,
            -0.5f,  0.5f, 0.0f,  0.0f, 1.0f
        };
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void crearCirculoBase() {
        int segmentos = 40;
        List<Float> vertices = new ArrayList<>();
        vertices.add(0.0f); vertices.add(0.0f); vertices.add(0.0f);
        for (int i = 0; i <= segmentos; i++) {
            float angulo = (float) (2.0f * Math.PI * i / segmentos);
            vertices.add((float) Math.cos(angulo) * 0.5f);
            vertices.add((float) Math.sin(angulo) * 0.5f);
            vertices.add(0.0f);
        }
        float[] arr = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) arr[i] = vertices.get(i);
        vaoCirculo = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoCirculo);
        vboCirculo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboCirculo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(arr.length);
        buffer.put(arr).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void crearTrianguloBase() {
        float[] vertices = {
             0.0f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f
        };
        vaoTriangulo = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoTriangulo);
        vboTriangulo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTriangulo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void cargarTexturaGameOver(String path) {
        int[] w = new int[1], h = new int[1], canales = new int[1];
        ByteBuffer imagen = STBImage.stbi_load(path, w, h, canales, 4);
        if (imagen == null) throw new RuntimeException("No se pudo cargar: " + path);
        textureGameOver = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureGameOver);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
            w[0], h[0], 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imagen);
        STBImage.stbi_image_free(imagen);
    }

    // ---------------------------------------------------------------
    // Estado de partida
    // ---------------------------------------------------------------

    private void resetGame() {
        bird1.reset(0.0f);
        bird2.reset(0.25f);
        timerSpawn         = 0.0f;
        puntaje            = 0;
        puntaje2           = 0;
        VELOCIDAD_TUBERIAS = 0.62f;
        started            = false;
        gameOver           = false;
        tuberias.clear();
        actualizarTitulo();
    }

    // ---------------------------------------------------------------
    // Títulos de ventana
    // ---------------------------------------------------------------

    private void actualizarTituloMenu() {
        GLFW.glfwSetWindowTitle(window,
            "Flappy Bird — MENU | FLECHAS para navegar | SPACE/ENTER para elegir");
    }

    private void actualizarTitulo() {
        String base = "Jugador 1: " + puntaje + " | Jugador 2: " + puntaje2;
        if (!started)      GLFW.glfwSetWindowTitle(window, base + " | SPACE para empezar");
        else if (gameOver) GLFW.glfwSetWindowTitle(window,
            base + " | GAME OVER - SPACE/R para volver al menú | M = Menú");
        else               GLFW.glfwSetWindowTitle(window, base + " | M = Menú");
    }

    // ---------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------

    /** Centraliza la lógica de volver al menú. */
    private void irAlMenu() {
        enMenu = true;
        menu.reset();
        actualizarTituloMenu();
    }

    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        // --- Tecla M: volver al menú en cualquier momento ---
        boolean mAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        if (mAhora && !prevM) {
            irAlMenu();
        }
        prevM = mAhora;

        // --- Teclas de juego ---
        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceAhora && !prevSpace) {
            if (gameOver) {
                // SPACE en game over → volver al menú para elegir modo
                irAlMenu();
            } else {
                started = true;
                bird1.saltar();
            }
        }
        prevSpace = spaceAhora;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            if (dosJugadores) {
                started = true;
                bird2.saltar();
            }
        }

        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rAhora && !prevR && gameOver) {
            // R en game over → volver al menú para elegir modo
            irAlMenu();
        }
        prevR = rAhora;
    }

    // ---------------------------------------------------------------
    // Lógica de juego
    // ---------------------------------------------------------------

    private void actualizar(float dt) {
        if (!started || gameOver) return;

        bird1.actualizar(dt);
        if (dosJugadores) bird2.actualizar(dt);

        boolean bird1Muerto = !bird1.estaVivo();
        boolean bird2Muerto = !dosJugadores || !bird2.estaVivo();

        if (bird1Muerto && bird2Muerto) {
            gameOver = true;
            actualizarTitulo();
            return;
        }

        timerSpawn += dt;
        if (timerSpawn >= TIEMPO_ENTRE_TUBERIAS) {
            timerSpawn = 0.0f;
            spawnTuberia();
        }

        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.x -= VELOCIDAD_TUBERIAS * dt;

            if (
                bird1.estaVivo() &&
                t.x + (TUBERIA_ANCHO * 0.5f) < bird1.getX() &&
                !t.puntuada
            ) {
                t.puntuada = true;
                puntaje++;
                VELOCIDAD_TUBERIAS = 0.62f + (puntaje / 10) * 0.15f;
                actualizarTitulo();
            }

            if (
                dosJugadores &&
                bird2.estaVivo() &&
                t.x + (TUBERIA_ANCHO * 0.5f) < bird2.getX() &&
                !t.puntuada2
            ) {
                t.puntuada2 = true;
                puntaje2++;
                actualizarTitulo();
            }

            if (bird1.estaVivo() &&
                bird1.colisionaConTuberia(t.x, TUBERIA_ANCHO, t.gapCentroY, GAP_ALTO)) {
                bird1.morir();
            }

            if (dosJugadores &&
                bird2.estaVivo() &&
                bird2.colisionaConTuberia(t.x, TUBERIA_ANCHO, t.gapCentroY, GAP_ALTO)) {
                bird2.morir();
            }

            boolean p1Muerto = !bird1.estaVivo();
            boolean p2Muerto = !dosJugadores || !bird2.estaVivo();
            if (p1Muerto && p2Muerto) {
                gameOver = true;
                actualizarTitulo();
                return;
            }

            if (t.x + (TUBERIA_ANCHO * 0.5f) < -1.3f) it.remove();
        }

        nubes.actualizar(dt, VELOCIDAD_TUBERIAS);
        montanas.actualizar(dt, VELOCIDAD_TUBERIAS);
    }

    private void spawnTuberia() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, gapCentro));
    }

    // ---------------------------------------------------------------
    // Render — orden de capas de fondo a frente
    // ---------------------------------------------------------------

    private void render() {
        // 1. Fondo azul cielo.
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programa);

        // 2. Nubes.
        nubes.render(vao, uUsarTexturaLocation);

        // 3. Montañas.
        GL20.glUseProgram(programa);
        montanas.render();

        // 4. Tuberías.
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        for (Tuberia t : tuberias) {
            float gapTop    = t.gapCentroY + (GAP_ALTO * 0.5f);
            float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);

            float altoSuperior = 1.0f - gapTop;
            if (altoSuperior > 0.0f) {
                float yCentroSup = gapTop + (altoSuperior * 0.5f);
                dibujarRect(t.x, yCentroSup, TUBERIA_ANCHO, altoSuperior, 0.18f, 0.70f, 0.25f);
                float luzAncho = TUBERIA_ANCHO * 0.12f;
                dibujarRect(t.x + (TUBERIA_ANCHO * 0.5f) - (luzAncho * 0.5f), yCentroSup, luzAncho, altoSuperior, 0.45f, 0.90f, 0.45f);
                dibujarRect(t.x - (TUBERIA_ANCHO * 0.5f) + (luzAncho * 0.5f), yCentroSup, luzAncho, altoSuperior, 0.08f, 0.40f, 0.12f);
                float bocaAlto = 0.035f;
                dibujarRect(t.x, gapTop + (bocaAlto * 0.5f), TUBERIA_ANCHO + 0.01f, bocaAlto, 0.05f, 0.28f, 0.08f);
                dibujarRect(t.x, gapTop + bocaAlto + 0.012f, TUBERIA_ANCHO * 0.6f, 0.008f, 0.55f, 0.95f, 0.55f);
            }

            float altoInferior = gapBottom + 1.0f;
            if (altoInferior > 0.0f) {
                float yCentroInf = -1.0f + (altoInferior * 0.5f);
                dibujarRect(t.x, yCentroInf, TUBERIA_ANCHO, altoInferior, 0.18f, 0.70f, 0.25f);
                float luzAncho = TUBERIA_ANCHO * 0.12f;
                dibujarRect(t.x + (TUBERIA_ANCHO * 0.5f) - (luzAncho * 0.5f), yCentroInf, luzAncho, altoInferior, 0.45f, 0.90f, 0.45f);
                dibujarRect(t.x - (TUBERIA_ANCHO * 0.5f) + (luzAncho * 0.5f), yCentroInf, luzAncho, altoInferior, 0.08f, 0.40f, 0.12f);
                float bocaAlto = 0.035f;
                dibujarRect(t.x, gapBottom - (bocaAlto * 0.5f), TUBERIA_ANCHO + 0.01f, bocaAlto, 0.05f, 0.28f, 0.08f);
                dibujarRect(t.x, gapBottom - bocaAlto - 0.012f, TUBERIA_ANCHO * 0.6f, 0.008f, 0.55f, 0.95f, 0.55f);
            }
        }

        // 5. Pájaros.
        bird1.render();
        if (dosJugadores) bird2.render();

        // 6. HUD de puntaje.
        hud.render(puntaje, puntaje2, VELOCIDAD_TUBERIAS);

        // 7. Overlay game over.
        if (gameOver) {
            GL30.glBindVertexArray(vao);
            dibujarRect(0.0f, 0.0f, 2.0f, 0.45f, 0.02f, 0.02f, 0.02f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glUniform1i(uUsarTexturaLocation, 1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureGameOver);
            dibujarRect(0.0f, 0.0f, 0.9f, 0.45f, 1, 1, 1);
            GL20.glUniform1i(uUsarTexturaLocation, 0);
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private void dibujarRect(float x, float y, float ancho, float alto,
                              float r, float g, float b) {
        GL20.glUniform1f(uRotationLocation, 0.0f);
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, ancho, alto);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    // ---------------------------------------------------------------
    // Bucle principal
    // ---------------------------------------------------------------

    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt    = Math.min(ahora - ultimoTiempo, 0.033f);
            ultimoTiempo = ahora;

            GLFW.glfwPollEvents();

            // *** Bloque de MENÚ ***
            if (enMenu) {
                // ESC cierra
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetWindowShouldClose(window, true);
                    continue;
                }

                GL20.glUseProgram(programa);
                menu.procesarInput(dt);
                menu.render();

                if (menu.isFinished()) {
                    // El usuario eligió un modo → arrancar el juego
                    dosJugadores = menu.esDosJugadores();
                    enMenu = false;
                    resetGame();
                }

                GLFW.glfwSwapBuffers(window);
                continue;
            }

            // *** Bloque de JUEGO ***
            procesarInput();

            // Si procesarInput() volvió al menú, saltar render del juego
            if (enMenu) {
                GLFW.glfwSwapBuffers(window);
                continue;
            }

            actualizar(dt);
            render();

            GLFW.glfwSwapBuffers(window);
        }
    }

    // ---------------------------------------------------------------
    // Limpieza
    // ---------------------------------------------------------------

    private void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new AppFlappyBird().run();
    }
}