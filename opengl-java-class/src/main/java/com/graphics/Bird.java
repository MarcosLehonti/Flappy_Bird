package com.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Bird:
 * Encapsula toda la lógica de un pájaro jugador:
 * - Posición y velocidad vertical.
 * - Física (gravedad + impulso de salto).
 * - Detección de colisión con tuberias y límites de pantalla.
 * - Renderizado (cuerpo, ojo, pico, ala, cola).
 */
public class Bird {

    // --- Constantes de tamaño ---
    public static final float ANCHO = 0.10f;
    public static final float ALTO  = 0.10f;

    // --- Constantes de física ---
    private static final float GRAVEDAD          = -1.9f;
    private static final float IMPULSO_SALTO     =  0.85f;
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // --- Posición fija horizontal y estado vertical ---
    private final float x;
    private float y;
    private float velY;

        // --- Variables de vivo ---

    private boolean vivo = true;

    // --- Color del cuerpo (RGB 0‑1) ---
    private final float r, g, b;

    // --- Referencias a uniforms del shader (se inyectan desde App) ---
    private final int uOffset;
    private final int uScale;
    private final int uColor;
    private final int uRotation;

    // --- VAOs de las figuras base (compartidos con App) ---
    private final int vaoQuad;
    private final int vaoCirculo;
    private final int vaoTriangulo;

    /**
     * @param x          Posición horizontal fija en NDC.
     * @param startY     Posición vertical inicial.
     * @param r,g,b      Color del cuerpo.
     * @param uOffset    Location del uniform uOffset.
     * @param uScale     Location del uniform uScale.
     * @param uColor     Location del uniform uColor.
     * @param uRotation  Location del uniform uRotation.
     * @param vaoQuad       VAO del quad base.
     * @param vaoCirculo    VAO del círculo base.
     * @param vaoTriangulo  VAO del triángulo base.
     */
    public Bird(
        float x, float startY,
        float r, float g, float b,
        int uOffset, int uScale, int uColor, int uRotation,
        int vaoQuad, int vaoCirculo, int vaoTriangulo
    ) {
        this.x = x;
        this.y = startY;
        this.r = r;
        this.g = g;
        this.b = b;
        this.uOffset    = uOffset;
        this.uScale     = uScale;
        this.uColor     = uColor;
        this.uRotation  = uRotation;
        this.vaoQuad       = vaoQuad;
        this.vaoCirculo    = vaoCirculo;
        this.vaoTriangulo  = vaoTriangulo;
    }

    // ---------------------------------------------------------------
    // Estado
    // ---------------------------------------------------------------

    /** Reinicia posición y velocidad al valor inicial. */
    public void reset(float startY) {
        this.y    = startY;
        this.velY = 0.0f;
        this.vivo = true;
    }

    /** Aplica impulso de salto. */
    public void saltar() {

        if (!vivo) return;

        velY = IMPULSO_SALTO;
    }
    // ---------------------------------------------------------------
    // Física
    // ---------------------------------------------------------------

    /**
     * Avanza la simulación física un paso de tiempo dt.
     * @return true si el pájaro chocó con el techo o el suelo.
     */
    public void actualizar(float dt) {

        if (!vivo) return;

        velY += GRAVEDAD * dt;

        if (velY < VELOCIDAD_MAX_CAIDA) {
            velY = VELOCIDAD_MAX_CAIDA;
        }

        y += velY * dt;

        float top    = y + (ALTO * 0.5f);
        float bottom = y - (ALTO * 0.5f);

        if (top >= 1.0f || bottom <= -1.0f) {
            vivo = false;
        }
    }
    // ---------------------------------------------------------------
    // Colisión
    // ---------------------------------------------------------------

    /**
     * Comprueba colisión AABB con una tubería.
     *
     * @param tuberiaX       Centro horizontal de la tubería.
     * @param tuberiaAncho   Ancho de la tubería.
     * @param gapCentroY     Centro vertical del hueco.
     * @param gapAlto        Alto del hueco.
     * @return true si hay colisión.
     */
    public boolean colisionaConTuberia(
        float tuberiaX, float tuberiaAncho,
        float gapCentroY, float gapAlto
    ) {
        float birdLeft   = x - (ANCHO * 0.5f);
        float birdRight  = x + (ANCHO * 0.5f);
        float birdBottom = y - (ALTO  * 0.5f);
        float birdTop    = y + (ALTO  * 0.5f);

        float pipeLeft  = tuberiaX - (tuberiaAncho * 0.5f);
        float pipeRight = tuberiaX + (tuberiaAncho * 0.5f);

        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) return false;

        float gapTop    = gapCentroY + (gapAlto * 0.5f);
        float gapBottom = gapCentroY - (gapAlto * 0.5f);
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    // ---------------------------------------------------------------
    // Renderizado
    // ---------------------------------------------------------------

    /** Dibuja el pájaro completo (cuerpo, ojo, pico, ala, cola). */
    public void render() {

        if (!vivo) return;

        // Cuerpo
        GL30.glBindVertexArray(vaoQuad);
        dibujarRect(x, y, ANCHO, ALTO, r, g, b);

        // Ojo blanco
        dibujarCirculo(
            x + 0.025f, y + 0.02f,
            0.03f, 0.03f,
            1.0f, 1.0f, 1.0f
        );

        // Pupila
        dibujarCirculo(
            x + 0.026f, y + 0.02f,
            0.015f, 0.015f,
            0.0f, 0.0f, 0.0f
        );

        // Pico
        dibujarTriangulo(
            x + 0.07f, y,
            0.04f, 0.04f,
            (float) Math.toRadians(-90),
            1.0f, 0.5f, 0.0f
        );

        // Ala exterior
        dibujarTriangulo(
            x - 0.025f, y - 0.01f,
            0.06f, 0.05f,
            (float) Math.toRadians(90),
            1.0f, 0.5f, 0.0f
        );

        // Ala interior (acento azul)
        dibujarTriangulo(
            x - 0.01f, y - 0.01f,
            0.04f, 0.03f,
            (float) Math.toRadians(90),
            0.0f, 0.4f, 1.0f
        );

        // Cola
        GL30.glBindVertexArray(vaoQuad);
        dibujarRect(
            x - 0.080f, y - 0.005f,
            0.06f, 0.035f,
            1.0f, 0.5f, 0.0f
        );
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public float getX()   { return x; }
    public float getY()   { return y; }
    public boolean estaVivo() {
    return vivo;
    }

    public void morir() {
        vivo = false;
    }

    // ---------------------------------------------------------------
    // Helpers privados de dibujo (delegan en uniforms del shader)
    // ---------------------------------------------------------------

    private void dibujarRect(
        float x, float y, float ancho, float alto,
        float r, float g, float b
    ) {
        GL20.glUniform1f(uRotation, 0.0f);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    private void dibujarCirculo(
        float x, float y, float ancho, float alto,
        float r, float g, float b
    ) {
        GL20.glUniform1f(uRotation, 0.0f);
        GL30.glBindVertexArray(vaoCirculo);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 42);
        GL30.glBindVertexArray(0);
    }

    private void dibujarTriangulo(
        float x, float y, float ancho, float alto,
        float rotacion,
        float r, float g, float b
    ) {
        GL30.glBindVertexArray(vaoTriangulo);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform1f(uRotation, rotacion);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        GL30.glBindVertexArray(0);
    }
}