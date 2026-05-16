package com.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Hud:
 * ---------------------------------------------------------
 * Dibuja toda la interfaz superior del juego.
 *
 * Elementos:
 * - Barra superior
 * - Puntaje jugador 1
 * - Puntaje jugador 2
 * - Velocidad actual de tuberías
 *
 * Los números se renderizan usando segmentos rectangulares
 * estilo display digital.
 *
 * IMPORTANTE:
 * ---------------------------------------------------------
 * Se agregó soporte para mostrar:
 *
 *     VELOCIDAD_TUBERIAS
 *
 * usando el mismo sistema visual de números digitales.
 *
 * SOLO debes modificar:
 *
 *     1. Esta clase HUD
 *     2. El método hud.render(...)
 *
 * NUEVO:
 *
 * Antes:
 *     hud.render(puntaje, puntaje2);
 *
 * Ahora:
 *     hud.render(puntaje, puntaje2, VELOCIDAD_TUBERIAS);
 *
 * ---------------------------------------------------------
 */
public class Hud {

    private final int uOffset;
    private final int uScale;
    private final int uColor;
    private final int uRotation;

    private final int vaoQuad;

    public Hud(
        int uOffset,
        int uScale,
        int uColor,
        int uRotation,
        int vaoQuad
    ) {
        this.uOffset   = uOffset;
        this.uScale    = uScale;
        this.uColor    = uColor;
        this.uRotation = uRotation;
        this.vaoQuad   = vaoQuad;
    }

    // ---------------------------------------------------------
    // Render principal
    // ---------------------------------------------------------

    public void render(
        int puntaje1,
        int puntaje2,
        float velocidad
    ) {

        GL30.glBindVertexArray(vaoQuad);

        // -----------------------------------------------------
        // Barra superior
        // -----------------------------------------------------

        dibujarRect(
            0.0f,
            0.90f,
            2.0f,
            0.20f,
            0.05f,
            0.05f,
            0.05f
        );

        // -----------------------------------------------------
        // Panel jugador 1
        // -----------------------------------------------------

        dibujarRect(
            -0.55f,
            0.90f,
            0.30f,
            0.10f,
            0.95f,
            0.80f,
            0.15f
        );

        // -----------------------------------------------------
        // Panel jugador 2
        // -----------------------------------------------------

        dibujarRect(
            0.55f,
            0.90f,
            0.30f,
            0.10f,
            0.15f,
            0.75f,
            1.0f
        );

        // -----------------------------------------------------
        // Panel velocidad
        // -----------------------------------------------------

        dibujarRect(
            0.0f,
            0.78f,
            0.42f,
            0.09f,
            0.20f,
            0.20f,
            0.20f
        );

        // -----------------------------------------------------
        // Puntajes
        // -----------------------------------------------------

        dibujarNumero(
            puntaje1,
            -0.15f,
            0.90f,
            0.045f
        );

        dibujarNumero(
            puntaje2,
             0.15f,
             0.90f,
             0.045f
        );

        // -----------------------------------------------------
        // Velocidad
        // -----------------------------------------------------
        // Multiplicamos x100 para verla mejor:
        //
        // 0.62 -> 62
        // 0.77 -> 77
        // etc
        // -----------------------------------------------------

        int velocidadVisual = (int)(velocidad * 100);

        dibujarNumero(
            velocidadVisual,
            -0.05f,
            0.78f,
            0.035f
        );
    }

    // ---------------------------------------------------------
    // Dibuja un número completo
    // ---------------------------------------------------------

    private void dibujarNumero(
        int numero,
        float x,
        float y,
        float escala
    ) {

        String texto = String.valueOf(numero);

        for (int i = 0; i < texto.length(); i++) {

            int digito = texto.charAt(i) - '0';

            dibujarDigito(
                digito,
                x + (i * escala * 1.8f),
                y,
                escala
            );
        }
    }

    // ---------------------------------------------------------
    // Dibuja un dígito estilo display digital
    // ---------------------------------------------------------

    private void dibujarDigito(
        int numero,
        float x,
        float y,
        float s
    ) {

        boolean top = false;
        boolean middle = false;
        boolean bottom = false;

        boolean topLeft = false;
        boolean topRight = false;

        boolean bottomLeft = false;
        boolean bottomRight = false;

        switch (numero) {

            case 0:
                top = true;
                bottom = true;
                topLeft = true;
                topRight = true;
                bottomLeft = true;
                bottomRight = true;
                break;

            case 1:
                topRight = true;
                bottomRight = true;
                break;

            case 2:
                top = true;
                middle = true;
                bottom = true;
                topRight = true;
                bottomLeft = true;
                break;

            case 3:
                top = true;
                middle = true;
                bottom = true;
                topRight = true;
                bottomRight = true;
                break;

            case 4:
                middle = true;
                topLeft = true;
                topRight = true;
                bottomRight = true;
                break;

            case 5:
                top = true;
                middle = true;
                bottom = true;
                topLeft = true;
                bottomRight = true;
                break;

            case 6:
                top = true;
                middle = true;
                bottom = true;
                topLeft = true;
                bottomLeft = true;
                bottomRight = true;
                break;

            case 7:
                top = true;
                topRight = true;
                bottomRight = true;
                break;

            case 8:
                top = true;
                middle = true;
                bottom = true;
                topLeft = true;
                topRight = true;
                bottomLeft = true;
                bottomRight = true;
                break;

            case 9:
                top = true;
                middle = true;
                bottom = true;
                topLeft = true;
                topRight = true;
                bottomRight = true;
                break;
        }

        float grosor = s * 0.20f;

        // Segmento superior
        if (top) {
            dibujarRect(
                x,
                y + s,
                s,
                grosor,
                1, 1, 1
            );
        }

        // Segmento medio
        if (middle) {
            dibujarRect(
                x,
                y,
                s,
                grosor,
                1, 1, 1
            );
        }

        // Segmento inferior
        if (bottom) {
            dibujarRect(
                x,
                y - s,
                s,
                grosor,
                1, 1, 1
            );
        }

        // Izquierda arriba
        if (topLeft) {
            dibujarRect(
                x - (s * 0.5f),
                y + (s * 0.5f),
                grosor,
                s,
                1, 1, 1
            );
        }

        // Derecha arriba
        if (topRight) {
            dibujarRect(
                x + (s * 0.5f),
                y + (s * 0.5f),
                grosor,
                s,
                1, 1, 1
            );
        }

        // Izquierda abajo
        if (bottomLeft) {
            dibujarRect(
                x - (s * 0.5f),
                y - (s * 0.5f),
                grosor,
                s,
                1, 1, 1
            );
        }

        // Derecha abajo
        if (bottomRight) {
            dibujarRect(
                x + (s * 0.5f),
                y - (s * 0.5f),
                grosor,
                s,
                1, 1, 1
            );
        }
    }

    // ---------------------------------------------------------
    // Helper dibujo rectángulo
    // ---------------------------------------------------------

    private void dibujarRect(
        float x,
        float y,
        float ancho,
        float alto,
        float r,
        float g,
        float b
    ) {

        GL20.glUniform1f(uRotation, 0.0f);

        GL20.glUniform2f(uOffset, x, y);

        GL20.glUniform2f(uScale, ancho, alto);

        GL20.glUniform3f(uColor, r, g, b);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }
}