package com.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Menu:
 * ---------------------------------------------------------
 * Pantalla de menú inicial del juego Flappy Bird.
 *
 * Muestra:
 *   - Título "FLAPPY BIRD" en letras digitales grandes
 *   - Opción "1 JUGADOR" (seleccionable con SPACE)
 *   - Opción "2 JUGADORES" (seleccionable con W)
 *   - Instrucciones de controles
 *   - Decoraciones visuales (pájaros de muestra)
 *
 * Estados posibles:
 *   NINGUNO   → menú activo, esperando selección
 *   UN_JUGADOR  → el usuario eligió 1 jugador
 *   DOS_JUGADORES → el usuario eligió 2 jugadores
 *
 * Uso desde AppFlappyBird:
 * ---------------------------------------------------------
 *
 *   Menu menu = new Menu(window, uOffset, uScale, uColor, uRotation, vao, vaoCirculo, vaoTriangulo);
 *
 *   // En el loop:
 *   if (!menu.isFinished()) {
 *       menu.procesarInput();
 *       menu.render();
 *       return; // no avanzar al juego todavía
 *   }
 *
 *   boolean dosJugadores = menu.esDosJugadores();
 *
 * ---------------------------------------------------------
 */
public class Menu {

    // -------------------------------------------------------
    // Modo de juego elegido
    // -------------------------------------------------------

    public enum Modo {
        NINGUNO,
        UN_JUGADOR,
        DOS_JUGADORES
    }

    // -------------------------------------------------------
    // Referencias al contexto OpenGL
    // -------------------------------------------------------

    private final long window;

    private final int uOffset;
    private final int uScale;
    private final int uColor;
    private final int uRotation;

    private final int vaoQuad;
    private final int vaoCirculo;
    private final int vaoTriangulo;

    // -------------------------------------------------------
    // Estado del menú
    // -------------------------------------------------------

    private Modo modoElegido = Modo.NINGUNO;

    // Opción resaltada: 0 = 1 jugador, 1 = 2 jugadores
    private int opcionSeleccionada = 0;

    // Para detectar flancos de tecla (evitar repetición)
    private boolean prevSpace  = false;
    private boolean prevW      = false;
    private boolean prevUp     = false;
    private boolean prevDown   = false;
    private boolean prevEnter  = false;

    // Animación de los pájaros del menú
    private float tiempoAnimacion = 0.0f;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    public Menu(
        long window,
        int uOffset,
        int uScale,
        int uColor,
        int uRotation,
        int vaoQuad,
        int vaoCirculo,
        int vaoTriangulo
    ) {
        this.window       = window;
        this.uOffset      = uOffset;
        this.uScale       = uScale;
        this.uColor       = uColor;
        this.uRotation    = uRotation;
        this.vaoQuad      = vaoQuad;
        this.vaoCirculo   = vaoCirculo;
        this.vaoTriangulo = vaoTriangulo;
    }

    // -------------------------------------------------------
    // Estado público
    // -------------------------------------------------------

    /** @return true si el usuario ya eligió un modo de juego. */
    public boolean isFinished() {
        return modoElegido != Modo.NINGUNO;
    }

    /** @return true si el modo elegido es 2 jugadores. */
    public boolean esDosJugadores() {
        return modoElegido == Modo.DOS_JUGADORES;
    }

    /** Reinicia el menú para volver a mostrarlo. */
    public void reset() {
        modoElegido          = Modo.NINGUNO;
        opcionSeleccionada   = 0;
        tiempoAnimacion      = 0.0f;
        prevSpace = prevW = prevUp = prevDown = prevEnter = false;
    }

    // -------------------------------------------------------
    // Input
    // -------------------------------------------------------

    public void procesarInput(float dt) {
        tiempoAnimacion += dt;

        // Navegar con flechas
        boolean upAhora   = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP)   == GLFW.GLFW_PRESS;
        boolean downAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN)  == GLFW.GLFW_PRESS;

        if (upAhora && !prevUp)     opcionSeleccionada = 0;
        if (downAhora && !prevDown) opcionSeleccionada = 1;

        prevUp   = upAhora;
        prevDown = downAhora;

        // SPACE → siempre confirma opción 0 (1 jugador) o la opción resaltada
        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceAhora && !prevSpace) {
            if (opcionSeleccionada == 0) {
                modoElegido = Modo.UN_JUGADOR;
            } else {
                modoElegido = Modo.DOS_JUGADORES;
            }
        }
        prevSpace = spaceAhora;

        // W → atajo directo para 2 jugadores (resalta y confirma)
        boolean wAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        if (wAhora && !prevW) {
            opcionSeleccionada = 1;
            modoElegido = Modo.DOS_JUGADORES;
        }
        prevW = wAhora;

        // ENTER → confirma la opción resaltada
        boolean enterAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        if (enterAhora && !prevEnter) {
            if (opcionSeleccionada == 0) {
                modoElegido = Modo.UN_JUGADOR;
            } else {
                modoElegido = Modo.DOS_JUGADORES;
            }
        }
        prevEnter = enterAhora;
    }

    // -------------------------------------------------------
    // Render
    // -------------------------------------------------------

    public void render() {

        // Fondo azul cielo (igual que el juego)
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL30.glBindVertexArray(vaoQuad);

        // ---------------------------------------------------
        // Panel central oscuro
        // ---------------------------------------------------
        dibujarRect(0.0f, 0.0f, 1.60f, 1.80f, 0.05f, 0.05f, 0.10f);

        // ---------------------------------------------------
        // Borde decorativo del panel
        // ---------------------------------------------------
        // Borde superior
        dibujarRect(0.0f,  0.92f, 1.62f, 0.04f, 0.98f, 0.80f, 0.10f);
        // Borde inferior
        dibujarRect(0.0f, -0.92f, 1.62f, 0.04f, 0.98f, 0.80f, 0.10f);
        // Borde izquierdo
        dibujarRect(-0.82f, 0.0f, 0.04f, 1.86f, 0.98f, 0.80f, 0.10f);
        // Borde derecho
        dibujarRect( 0.82f, 0.0f, 0.04f, 1.86f, 0.98f, 0.80f, 0.10f);

        // ---------------------------------------------------
        // Título: "FLAPPY" en la línea superior
        // ---------------------------------------------------
        dibujarLetras("FLAPPY", -0.65f, 0.68f, 0.09f, 0.98f, 0.85f, 0.10f);

        // ---------------------------------------------------
        // Línea separadora bajo el título
        // ---------------------------------------------------
        dibujarRect(0.0f, 0.38f, 1.30f, 0.01f, 0.98f, 0.80f, 0.10f);

        // ---------------------------------------------------
        // Pájaros animados de muestra (uno amarillo, uno azul)
        // ---------------------------------------------------
        float bobY1 = (float) Math.sin(tiempoAnimacion * 2.5f) * 0.035f;
        float bobY2 = (float) Math.sin(tiempoAnimacion * 2.5f + Math.PI) * 0.035f;

        dibujarPajaroDecorativo(-0.18f, 0.22f + bobY1, 0.98f, 0.85f, 0.10f);
        dibujarPajaroDecorativo( 0.18f, 0.22f + bobY2, 0.20f, 0.80f, 1.00f);

        // ---------------------------------------------------
        // Opción 1: 1 JUGADOR
        // ---------------------------------------------------
        boolean op1Sel = (opcionSeleccionada == 0);

        // Fondo de la opción
        float r1 = op1Sel ? 0.98f : 0.15f;
        float g1 = op1Sel ? 0.80f : 0.15f;
        float b1 = op1Sel ? 0.10f : 0.15f;
        dibujarRect(0.0f, -0.10f, 1.30f, 0.13f, r1, g1, b1);

        // Borde de la opción
        if (op1Sel) {
            dibujarRect(0.0f, -0.10f, 1.32f, 0.15f, 1.0f, 1.0f, 0.0f);
            dibujarRect(0.0f, -0.10f, 1.30f, 0.13f, r1, g1, b1);
        }

        // Texto "1 JUGADOR"
        float tc1 = op1Sel ? 0.0f : 0.8f;
        dibujarNumeroSimple(1, -0.55f, -0.10f, 0.040f, tc1, tc1, tc1);
        dibujarLetras("JUGADOR", -0.35f, -0.10f, 0.040f, tc1, tc1, tc1);

        // Controles de la opción 1
        dibujarLetras("SPACE", -0.55f, -0.27f, 0.025f, 0.70f, 0.70f, 0.70f);

        // ---------------------------------------------------
        // Opción 2: 2 JUGADORES
        // ---------------------------------------------------
        boolean op2Sel = (opcionSeleccionada == 1);

        float r2 = op2Sel ? 0.10f : 0.15f;
        float g2 = op2Sel ? 0.60f : 0.15f;
        float b2 = op2Sel ? 1.00f : 0.15f;
        dibujarRect(0.0f, -0.52f, 1.30f, 0.13f, r2, g2, b2);

        if (op2Sel) {
            dibujarRect(0.0f, -0.52f, 1.32f, 0.15f, 1.0f, 1.0f, 0.0f);
            dibujarRect(0.0f, -0.52f, 1.30f, 0.13f, r2, g2, b2);
        }

        float tc2 = op2Sel ? 1.0f : 0.8f;
        dibujarNumeroSimple(2, -0.55f, -0.52f, 0.040f, tc2, tc2, tc2);
        dibujarLetras("JUGADORES", -0.35f, -0.52f, 0.040f, tc2, tc2, tc2);

        // Controles de la opción 2
        dibujarLetraEspecial('W', -0.55f, -0.69f, 0.025f, 0.70f, 0.70f, 0.70f);
        dibujarLetras(" SPACE", -0.55f + 0.025f * 1.6f * 2, -0.69f,
                    0.025f, 0.70f, 0.70f, 0.70f);
        // ---------------------------------------------------
        // Instrucción de navegación al fondo
        // ---------------------------------------------------
        dibujarLetras("FLECHAS PARA NAVEGAR", -0.68f, -0.82f, 0.020f, 0.50f, 0.50f, 0.50f);
    }

    // -------------------------------------------------------
    // Dibujar pájaro decorativo en el menú
    // -------------------------------------------------------

    private void dibujarPajaroDecorativo(float px, float py, float r, float g, float b) {
        float s = 0.10f;

        // Cuerpo
        GL30.glBindVertexArray(vaoQuad);
        dibujarRect(px, py, s, s, r, g, b);

        // Ojo blanco
        dibujarCirculo(px + 0.025f, py + 0.020f, 0.030f, 0.030f, 1.0f, 1.0f, 1.0f);

        // Pupila
        dibujarCirculo(px + 0.026f, py + 0.020f, 0.015f, 0.015f, 0.0f, 0.0f, 0.0f);

        // Pico
        dibujarTriangulo(px + 0.070f, py, 0.040f, 0.040f,
            (float) Math.toRadians(-90), 1.0f, 0.5f, 0.0f);

        // Ala exterior
        dibujarTriangulo(px - 0.025f, py - 0.010f, 0.060f, 0.050f,
            (float) Math.toRadians(90), 1.0f, 0.5f, 0.0f);

        // Cola
        GL30.glBindVertexArray(vaoQuad);
        dibujarRect(px - 0.080f, py - 0.005f, 0.060f, 0.035f, 1.0f, 0.5f, 0.0f);
    }

    // -------------------------------------------------------
    // Sistema de letras digitales (subconjunto del alfabeto)
    // -------------------------------------------------------

    /**
     * Dibuja una cadena de texto usando el sistema de segmentos digitales.
     * Soporta: A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
     *          0-9 y espacio.
     */
    private void dibujarLetras(String texto, float x, float y, float s,
                                float r, float g, float b) {
        float avance = s * 1.6f;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == ' ') continue;
            dibujarLetra(c, x + i * avance, y, s, r, g, b);
        }
    }

    private void dibujarLetra(char c, float x, float y, float s,
                               float r, float gr, float b) {
        float grosor = s * 0.18f;

        // Segmentos de un display de 7 segmentos extendido a letras:
        // top, middle, bottom, topLeft, topRight, bottomLeft, bottomRight
        boolean top = false, mid = false, bot = false;
        boolean tL  = false, tR  = false, bL  = false, bR  = false;

        switch (Character.toUpperCase(c)) {
            case '0': top=true; bot=true; tL=true; tR=true; bL=true; bR=true; break;
            case '1': tR=true; bR=true; break;
            case '2': top=true; mid=true; bot=true; tR=true; bL=true; break;
            case '3': top=true; mid=true; bot=true; tR=true; bR=true; break;
            case '4': mid=true; tL=true; tR=true; bR=true; break;
            case '5': top=true; mid=true; bot=true; tL=true; bR=true; break;
            case '6': top=true; mid=true; bot=true; tL=true; bL=true; bR=true; break;
            case '7': top=true; tR=true; bR=true; break;
            case '8': top=true; mid=true; bot=true; tL=true; tR=true; bL=true; bR=true; break;
            case '9': top=true; mid=true; bot=true; tL=true; tR=true; bR=true; break;
            case 'A': top=true; mid=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'B': top=true; mid=true; bot=true; tL=true; bL=true; bR=true; break;
            case 'C': top=true; bot=true; tL=true; bL=true; break;
            case 'D': top=true; bot=true; tR=true; bL=true; bR=true; break;
            case 'E': top=true; mid=true; bot=true; tL=true; bL=true; break;
            case 'F': top=true; mid=true; tL=true; bL=true; break;
            case 'G': top=true; mid=true; bot=true; tL=true; bL=true; bR=true; break;
            case 'H': mid=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'I': top=true; bot=true; tR=true; bR=true; break;
            case 'J': bot=true; tR=true; bL=true; bR=true; break;
            case 'K': top=true; mid=true; tL=true; bL=true; tR=true; break; // aproximado
            case 'L': bot=true; tL=true; bL=true; break;
            case 'M': top=true; tL=true; tR=true; bL=true; bR=true; break; // aproximado
            case 'N': top=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'O': top=true; bot=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'P': top=true; mid=true; tL=true; tR=true; bL=true; break;
            case 'Q': top=true; mid=true; bot=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'R': top=true; mid=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'S': top=true; mid=true; bot=true; tL=true; bR=true; break;
            case 'T': top=true; tL=true; bL=true; break; // aproximado
            case 'U': bot=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'V': bot=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'W': bot=true; mid=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'X': mid=true; tL=true; tR=true; bL=true; bR=true; break;
            case 'Y': mid=true; tL=true; tR=true; bR=true; break;
            case 'Z': top=true; mid=true; bot=true; tR=true; bL=true; break;
            default: break;
        }

        if (top) dibujarRect(x,            y + s,       s, grosor, r, gr, b);
        if (mid) dibujarRect(x,            y,           s, grosor, r, gr, b);
        if (bot) dibujarRect(x,            y - s,       s, grosor, r, gr, b);
        if (tL)  dibujarRect(x - s*0.5f,  y + s*0.5f,  grosor, s, r, gr, b);
        if (tR)  dibujarRect(x + s*0.5f,  y + s*0.5f,  grosor, s, r, gr, b);
        if (bL)  dibujarRect(x - s*0.5f,  y - s*0.5f,  grosor, s, r, gr, b);
        if (bR)  dibujarRect(x + s*0.5f,  y - s*0.5f,  grosor, s, r, gr, b);
    }

    private void dibujarNumeroSimple(int n, float x, float y, float s,
                                     float r, float g, float b) {
        dibujarLetra((char)('0' + n), x, y, s, r, g, b);
    }

    /**
     * Versiones mejoradas de letras problemáticas en 7 segmentos:
     *   'b' → como un 6 sin segmento superior  (mid+bot+tL+bL+bR)
     *   'I' → barra vertical centrada           (tR+bR  con top+bot)
     *   'r' → solo top+mid+tL+bL               (r minúscula de display)
     *   'd' → mid+bot+tR+bL+bR                 (d minúscula de display)
     */
    private void dibujarLetraEspecial(char c, float x, float y, float s,
                                       float r, float gr, float b) {
        float grosor = s * 0.18f;
        boolean top=false, mid=false, bot=false;
        boolean tL=false, tR=false, bL=false, bR=false;
        switch (c) {
            case 'b': mid=true; bot=true; tL=true; bL=true; bR=true; break;
            case 'I': top=true; bot=true; tL=true; tR=true; bL=true; bR=true; break; // igual a 0 pero sin laterales → mejor legibilidad como I mayúscula clásica
            case 'r': top=true; mid=true; tL=true; bL=true; break;
            case 'd': mid=true; bot=true; tR=true; bL=true; bR=true; break;
            case 'W':
                bot = true;
                tL  = true;
                tR  = true;
                bL  = true;
                bR  = true;
                break;
            default:  dibujarLetra(c, x, y, s, r, gr, b); return;
        }
        if (top) dibujarRect(x,           y + s,      s,      grosor, r, gr, b);
        if (mid) dibujarRect(x,           y,          s,      grosor, r, gr, b);
        if (bot) dibujarRect(x,           y - s,      s,      grosor, r, gr, b);
        if (tL)  dibujarRect(x - s*0.5f,  y + s*0.5f, grosor, s,      r, gr, b);
        if (tR)  dibujarRect(x + s*0.5f,  y + s*0.5f, grosor, s,      r, gr, b);
        if (bL)  dibujarRect(x - s*0.5f,  y - s*0.5f, grosor, s,      r, gr, b);
        if (bR)  dibujarRect(x + s*0.5f,  y - s*0.5f, grosor, s,      r, gr, b);
    }

    // -------------------------------------------------------
    // Helpers de dibujo
    // -------------------------------------------------------

    private void dibujarRect(float x, float y, float ancho, float alto,
                              float r, float g, float b) {
        GL20.glUniform1f(uRotation, 0.0f);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    private void dibujarCirculo(float x, float y, float ancho, float alto,
                                 float r, float g, float b) {
        GL20.glUniform1f(uRotation, 0.0f);
        GL30.glBindVertexArray(vaoCirculo);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 42);
        GL30.glBindVertexArray(vaoQuad);
    }

    private void dibujarTriangulo(float x, float y, float ancho, float alto,
                                   float rotacion, float r, float g, float b) {
        GL30.glBindVertexArray(vaoTriangulo);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform1f(uRotation, rotacion);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        GL30.glBindVertexArray(vaoQuad);
    }
}