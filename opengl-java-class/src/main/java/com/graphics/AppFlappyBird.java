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
 *   MENU → JUGANDO → GAME OVER → MENU (con R o SPACE)
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
    private static final float BIRD3_X = -0.25f;
    
    // Parámetros de tuberías.
    private static final float TUBERIA_ANCHO         = 0.18f;
    private static final float GAP_ALTO              = 0.48f;
    private float              VELOCIDAD_TUBERIAS   = 0.62f;
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
    private Nubes  nubes;
    private Montanas montanas;
    private int      textureGameOver;

    // HUD de puntaje
    private Hud hud;

    // *** MENÚ DE INICIO ***
    private Menu   menu;
    private boolean enMenu        = true;   
    private boolean dosJugadores  = false; 
    private boolean tresJugadores = false; 

    // Pájaros.
    private Bird bird1;
    private Bird bird2;
    private Bird bird3; // Nuevo jugador 

    // Estado del juego.
    private float  timerSpawn;
    private int    puntaje;
    private int    puntaje2;
    private int    puntaje3; // Tercer puntaje añadido
    private boolean started;
    private boolean gameOver;
    private boolean prevSpace;
    private boolean prevR;
    private boolean prevM;   

    // Lista de obstáculos activos.
    private final List<Tuberia> tuberias = new ArrayList<>();
    private final Random         random   = new Random();

    /** Modelo de una tubería. */
    private static class Tuberia {
        float   x;
        float   gapCentroY;
        boolean puntuada;
        boolean puntuada2;
        boolean puntuada3; // Flag de puntuación para el jugador 3

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

        // Configuración de la ventana OpenGL 3.3 Core Profile
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

        // Inicializar Nubes
        nubes = new Nubes();
        nubes.init("src/main/resources/nube.png");

        // Inicializar Montañas
        montanas = new Montanas();
        montanas.init(
            uOffsetLocation,
            uScaleLocation,
            uColorLocation,
            uRotationLocation,
            vaoTriangulo
        );

        // Inicializar HUD de puntaje
        hud = new Hud(
            uOffsetLocation,
            uScaleLocation,
            uColorLocation,
            uRotationLocation,
            vao
        );

        // Inicializar Menú
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

        // Crear los tres pájaros con sus posiciones iniciales fijas y colores distintivos
        // Jugador 1: Amarillo
        bird1 = new Bird(
            BIRD_X,  0.0f,
            0.98f, 0.85f, 0.20f,
            uOffsetLocation, uScaleLocation, uColorLocation, uRotationLocation,
            vao, vaoCirculo, vaoTriangulo
        );
        // Jugador 2: Azul celeste
        bird2 = new Bird(
            BIRD2_X, 0.25f,
            0.20f, 0.80f, 1.00f,
            uOffsetLocation, uScaleLocation, uColorLocation, uRotationLocation,
            vao, vaoCirculo, vaoTriangulo
        );
        // Jugador 3: Rojo/Coral
        bird3 = new Bird(
            BIRD3_X, -0.25f,
            1.00f, 0.30f, 0.30f,
            uOffsetLocation, uScaleLocation, uColorLocation, uRotationLocation,
            vao, vaoCirculo, vaoTriangulo
        );

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
        bird3.reset(-0.25f);
        timerSpawn         = 0.0f;
        puntaje            = 0;
        puntaje2           = 0;
        puntaje3           = 0;
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
        String base = "P1: " + puntaje;
        if (dosJugadores || tresJugadores) {
            base += " | P2: " + puntaje2;
        }
        if (tresJugadores) {
            base += " | P3: " + puntaje3;
        }

        if (!started)      GLFW.glfwSetWindowTitle(window, base + " | SPACE para empezar");
        else if (gameOver) GLFW.glfwSetWindowTitle(window, base + " | GAME OVER - SPACE/R para volver al menú | M = Menú");
        else               GLFW.glfwSetWindowTitle(window, base + " | M = Menú");
    }

    // ---------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------

    private void irAlMenu() {
        enMenu = true;
        menu.reset();
        actualizarTituloMenu();
    }

    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            // Cerrar el juego de manera limpia
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        // Tecla M: Volver al menú
        boolean mAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_M) == GLFW.GLFW_PRESS;
        if (mAhora && !prevM) {
            irAlMenu();
        }
        prevM = mAhora;

        // --- Teclas de control para saltos ---
        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceAhora && !prevSpace) {
            if (gameOver) {
                irAlMenu();
            } else {
                started = true;
                bird1.saltar(); // J1 Salta con SPACE
            }
        }
        prevSpace = spaceAhora;

        // J2 Salta con W (disponible en modo 2 y 3 jugadores)
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            if (dosJugadores || tresJugadores) {
                started = true;
                bird2.saltar();
            }
        }

        // J3 Salta con Flecha ARRIBA (disponible solo en modo 3 jugadores)
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            if (tresJugadores) {
                started = true;
                bird3.saltar();
            }
        }

        // Tecla R para regresar desde Game Over
        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rAhora && !prevR && gameOver) {
            irAlMenu();
        }
        prevR = rAhora;
    }

    // ---------------------------------------------------------------
    // Lógica de juego (Actualización)
    // ---------------------------------------------------------------

    private void actualizar(float dt) {
        if (!started || gameOver) return;

        // Actualizar pájaros activos
        if (bird1.estaVivo()) bird1.actualizar(dt);
        if ((dosJugadores || tresJugadores) && bird2.estaVivo()) bird2.actualizar(dt);
        if (tresJugadores && bird3.estaVivo()) bird3.actualizar(dt);

        // Evaluar estados de muerte según el modo elegido
        boolean bird1Muerto = !bird1.estaVivo();
        boolean bird2Muerto = (!dosJugadores && !tresJugadores) || !bird2.estaVivo();
        boolean bird3Muerto = !tresJugadores || !bird3.estaVivo();

        // El juego termina SOLÓ cuando todos los competidores del modo mueren
        if (bird1Muerto && bird2Muerto && bird3Muerto) {
            gameOver = true;
            actualizarTitulo();
            return;
        }

        // Control del temporizador para spawnear obstáculos
        timerSpawn += dt;
        if (timerSpawn >= TIEMPO_ENTRE_TUBERIAS) {
            timerSpawn = 0.0f;
            spawnTuberia();
        }

        // Procesar tuberías activas
        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.x -= VELOCIDAD_TUBERIAS * dt;

            // --- Lógica de puntaje individual ---
            if (bird1.estaVivo() && t.x + (TUBERIA_ANCHO * 0.5f) < bird1.getX() && !t.puntuada) {
                t.puntuada = true;
                puntaje++;
                VELOCIDAD_TUBERIAS = 0.62f + ((puntaje + puntaje2 + puntaje3) / 15) * 0.12f; // Velocidad
                actualizarTitulo();

                //CONDICIONAL DE ACABAR EL JUEGO
                if (puntaje >= 10) {
                    gameOver = true;
                    actualizarTitulo();
                    return;
                }
            }

            if ((dosJugadores || tresJugadores) && bird2.estaVivo() && t.x + (TUBERIA_ANCHO * 0.5f) < bird2.getX() && !t.puntuada2) {
                t.puntuada2 = true;
                puntaje2++;
                actualizarTitulo();
                //CONDICINAL DE ACBAR EL JUEGO
                if (puntaje >= 10) {
                    gameOver = true;
                    actualizarTitulo();
                    return;
                }
            }

            if (tresJugadores && bird3.estaVivo() && t.x + (TUBERIA_ANCHO * 0.5f) < bird3.getX() && !t.puntuada3) {
                t.puntuada3 = true;
                puntaje3++;
                actualizarTitulo();
                //CONDICIONAL DE ACABAR EL JUEGO
                if (puntaje >= 10) {
                    gameOver = true;
                    actualizarTitulo();
                    return;
                }
            }

            // --- Lógica de Colisiones por cada pájaro ---
            if (bird1.estaVivo() && bird1.colisionaConTuberia(t.x, TUBERIA_ANCHO, t.gapCentroY, GAP_ALTO)) {
                bird1.morir();
            }

            if ((dosJugadores || tresJugadores) && bird2.estaVivo() && bird2.colisionaConTuberia(t.x, TUBERIA_ANCHO, t.gapCentroY, GAP_ALTO)) {
                bird2.morir();
            }

            if (tresJugadores && bird3.estaVivo() && bird3.colisionaConTuberia(t.x, TUBERIA_ANCHO, t.gapCentroY, GAP_ALTO)) {
                bird3.morir();
            }

            // Eliminar tuberías fuera del plano visible de NDC
            if (t.x + (TUBERIA_ANCHO * 0.5f) < -1.3f) {
                it.remove();
            }
        }

        nubes.actualizar(dt, VELOCIDAD_TUBERIAS);
        montanas.actualizar(dt, VELOCIDAD_TUBERIAS);
    }

    private void spawnTuberia() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, gapCentro));
    }

    // ---------------------------------------------------------------
    // Renderizado por capas
    // ---------------------------------------------------------------

    private void render() {
        // 1. Limpieza de pantalla con azul cielo
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programa);

        // 2. Nubes
        nubes.render(vao, uUsarTexturaLocation);

        // 3. Montañas
        GL20.glUseProgram(programa);
        montanas.render();

        // 4. Render de las Tuberías
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        for (Tuberia t : tuberias) {
            float gapTop    = t.gapCentroY + (GAP_ALTO * 0.5f);
            float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);

            // Tubo Superior
            float altoSuperior = 1.0f - gapTop;
            if (altoSuperior > 0.0f) {
                float yCentroSup = gapTop + (altoSuperior * 0.5f);
                dibujarRect(t.x, yCentroSup, TUBERIA_ANCHO, altoSuperior, 0.18f, 0.70f, 0.25f);
                float luzAncho = TUBERIA_ANCHO * 0.12f;
                dibujarRect(t.x + (TUBERIA_ANCHO * 0.5f) - (luzAncho * 0.5f), yCentroSup, luzAncho, altoSuperior, 0.45f, 0.90f, 0.45f);//luz
                dibujarRect(t.x - (TUBERIA_ANCHO * 0.5f) + (luzAncho * 0.5f), yCentroSup, luzAncho, altoSuperior, 0.08f, 0.40f, 0.12f);//sombra
                float bocaAlto = 0.035f;
                dibujarRect(t.x, gapTop + (bocaAlto * 0.5f), TUBERIA_ANCHO + 0.01f, bocaAlto, 0.05f, 0.28f, 0.08f);
                dibujarRect(t.x, gapTop + bocaAlto + 0.012f, TUBERIA_ANCHO * 0.6f, 0.008f, 0.55f, 0.95f, 0.55f);
            }

            // Tubo Inferior
            float altoInferior = gapBottom + 1.0f;
            if (altoInferior > 0.0f) {
                float yCentroInf = -1.0f + (altoInferior * 0.5f);
                dibujarRect(t.x, yCentroInf, TUBERIA_ANCHO, altoInferior, 0.18f, 0.70f, 0.25f);
                float luzAncho = TUBERIA_ANCHO * 0.12f;
                dibujarRect(t.x + (TUBERIA_ANCHO * 0.5f) - (luzAncho * 0.5f), yCentroInf, luzAncho, altoInferior, 0.45f, 0.90f, 0.45f);//luz
                dibujarRect(t.x - (TUBERIA_ANCHO * 0.5f) + (luzAncho * 0.5f), yCentroInf, luzAncho, altoInferior, 0.08f, 0.40f, 0.12f);//sombra
                float bocaAlto = 0.035f;
                dibujarRect(t.x, gapBottom - (bocaAlto * 0.5f), TUBERIA_ANCHO + 0.01f, bocaAlto, 0.05f, 0.28f, 0.08f);
                dibujarRect(t.x, gapBottom - bocaAlto - 0.012f, TUBERIA_ANCHO * 0.6f, 0.008f, 0.55f, 0.95f, 0.55f);
            }
        }

        // 5. Render de Pájaros (Solo si corresponden al modo de juego)
        bird1.render();
        if (dosJugadores || tresJugadores) {
            bird2.render();
        }
        if (tresJugadores) {
            bird3.render();
        }

        // 6. HUD de puntaje (Adaptado internamente si pasás los puntajes correspondientes)
        hud.render(
            puntaje,
            puntaje2,
            puntaje3,
            VELOCIDAD_TUBERIAS
        );

        // 7. Render de Game Over Overlay
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
    // Bucle principal de GLFW
    // ---------------------------------------------------------------

    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt    = Math.min(ahora - ultimoTiempo, 0.033f);
            ultimoTiempo = ahora;

            GLFW.glfwPollEvents();

            // Bloque del menú principal
            if (enMenu) {
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                    GLFW.glfwSetWindowShouldClose(window, true);
                    continue;
                }

                GL20.glUseProgram(programa);
                menu.procesarInput(dt);
                menu.render();

                if (menu.isFinished()) {
                    dosJugadores = menu.esDosJugadores();
                    tresJugadores = menu.esTresJugadores();
                    enMenu = false;
                    resetGame();
                }

                GLFW.glfwSwapBuffers(window);
                continue;
            }

            // Bloque interactivo del juego activo
            procesarInput();

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
    // Destrucción de Contexto
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