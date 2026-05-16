package com.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Montanas:
 * Cada montaña se construye con 3 triángulos que comparten
 * exactamente el mismo centro X y la misma BASE_Y, formando
 * una sola figura coherente:
 *
 *   1. Cuerpo completo      — color base verde oscuro, ancho total
 *   2. Cara derecha (luz)   — mismo centro, mitad del ancho, desplazado a la derecha → más claro
 *   3. Nieve en la punta    — mismo centro, ancho pequeño, arriba del todo
 *
 * Así la punta, la sombra y la nieve siempre coinciden.
 */
public class Montanas {

    private static final int   CANTIDAD   = 7;
    private static final float SEPARACION = 0.55f;

    // ---- Fila TRASERA ----
    private static final float ANCHO_TRASERA   = 0.42f;
    private static final float ALTO_TRASERA    = 0.32f;
    private static final float BASE_Y_TRASERA  = -1.0f + ALTO_TRASERA * 0.5f;

    // ---- Fila DELANTERA ----
    private static final float ANCHO_DELANTERA  = 0.60f;
    private static final float ALTO_DELANTERA   = 0.52f;
    private static final float BASE_Y_DELANTERA = -1.0f + ALTO_DELANTERA * 0.5f;

    private final float[] xTrasera   = new float[CANTIDAD];
    private final float[] xDelantera = new float[CANTIDAD];

    private int uOffset;
    private int uScale;
    private int uColor;
    private int uRotation;
    private int vaoTriangulo;

    public void init(
        int uOffset, int uScale, int uColor, int uRotation, int vaoTriangulo
    ) {
        this.uOffset      = uOffset;
        this.uScale       = uScale;
        this.uColor       = uColor;
        this.uRotation    = uRotation;
        this.vaoTriangulo = vaoTriangulo;

        for (int i = 0; i < CANTIDAD; i++) {
            xTrasera[i]   = -1.0f + i * SEPARACION + 0.10f;
            xDelantera[i] = -1.0f + i * SEPARACION - 0.05f;
        }
    }

    public void actualizar(float dt, float velocidad) {
        float velTrasera   = velocidad * 0.35f;
        float velDelantera = velocidad * 0.55f;

        for (int i = 0; i < CANTIDAD; i++) {
            xTrasera[i]   -= velTrasera   * dt;
            xDelantera[i] -= velDelantera * dt;

            if (xTrasera[i]   + (ANCHO_TRASERA   * 0.5f) < -1.2f) xTrasera[i]   += CANTIDAD * SEPARACION;
            if (xDelantera[i] + (ANCHO_DELANTERA * 0.5f) < -1.2f) xDelantera[i] += CANTIDAD * SEPARACION;
        }
    }

    public void render() {
        GL30.glBindVertexArray(vaoTriangulo);

        // ---- Fila TRASERA ----
        for (int i = 0; i < CANTIDAD; i++) {
            float x = xTrasera[i];
            float y = BASE_Y_TRASERA;
            float w = ANCHO_TRASERA;
            float h = ALTO_TRASERA;

            // 1. Cuerpo completo — verde oscuro
            dibujarTriangulo(x, y, w, h, 0.25f, 0.35f, 0.25f);

            // 2. Cara derecha (luz) — mismo centro X, mitad derecha más clara
            //    Se desplaza +w*0.13 para que la punta coincida y tape solo el lado derecho
            dibujarTriangulo(x + w * 0.13f, y, w * 0.55f, h, 0.35f, 0.47f, 0.35f);

            // 3. Nieve — mismo centro X, punta arriba alineada
            dibujarTriangulo(x, y + h * 0.32f, w * 0.36f, h * 0.30f, 0.88f, 0.92f, 0.95f);
        }

        // ---- Fila DELANTERA ----
        for (int i = 0; i < CANTIDAD; i++) {
            float x = xDelantera[i];
            float y = BASE_Y_DELANTERA;
            float w = ANCHO_DELANTERA;
            float h = ALTO_DELANTERA;

            // 1. Cuerpo completo — verde oscuro base
            dibujarTriangulo(x, y, w, h, 0.25f, 0.35f, 0.25f);

            // 2. Cara derecha (luz) — mismo centro X desplazado ligeramente a la derecha
            dibujarTriangulo(x + w * 0.53f, y, w * 0.35f, h, 0.38f, 0.50f, 0.36f);

            // 3. Nieve — mismo centro X, justo en la punta
            dibujarTriangulo(x, y + h * 0.32f, w * 0.38f, h * 0.32f, 0.90f, 0.94f, 0.97f);
        }

        GL30.glBindVertexArray(0);
    }

    private void dibujarTriangulo(
        float x, float y, float ancho, float alto,
        float r, float g, float b
    ) {
        GL20.glUniform1f(uRotation, 0.0f);
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform3f(uColor, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }
}