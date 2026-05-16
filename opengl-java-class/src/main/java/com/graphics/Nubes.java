package com.graphics;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

/**
 * Clase Nubes:
 * ------------------------------------------------------------
 * Esta clase se encarga de:
 *
 * - Administrar todas las nubes del escenario.
 * - Cargar la textura PNG de nube.
 * - Crear nuevas nubes aleatorias.
 * - Actualizar su movimiento.
 * - Dibujar las nubes usando OpenGL.
 *
 * Las nubes forman parte del fondo del escenario y
 * se mueven lentamente para generar efecto de profundidad.
 */
public class Nubes {

    /**
     * Clase interna Nube:
     * ------------------------------------------------------------
     * Representa una nube individual.
     *
     * Cada nube tiene:
     * - Posición horizontal (x)
     * - Posición vertical (y)
     * - Escala/tamaño
     */
    private static class Nube {

        // Posición horizontal en NDC.
        float x;

        // Posición vertical en NDC.
        float y;

        // Escala de tamaño de la nube.
        float escala;

        /**
         * Constructor de nube.
         *
         * @param x       Posición horizontal.
         * @param y       Posición vertical.
         * @param escala  Tamaño de la nube.
         */
        Nube(float x, float y, float escala) {
            this.x = x;
            this.y = y;
            this.escala = escala;
        }
    }

    // Lista de nubes activas en pantalla.
    private final List<Nube> nubes = new ArrayList<>();

    // Generador de números aleatorios.
    private final Random random = new Random();

    // ID de textura OpenGL.
    private int textureId;

    // Temporizador para generar nuevas nubes.
    private float timerNube = 0f;

    /**
     * Inicializa el sistema de nubes.
     *
     * - Carga la textura PNG.
     * - Crea las nubes iniciales.
     *
     * @param pathTextura Ruta de la textura.
     */
    public void init(String pathTextura) {

        cargarTextura(pathTextura);

        iniciarNubes();
    }

    /**
     * Carga la textura PNG usando STBImage.
     *
     * @param path Ruta del archivo PNG.
     */
    private void cargarTextura(String path) {

        // Arreglos para almacenar ancho, alto y canales.
        int[] w = new int[1];
        int[] h = new int[1];
        int[] canales = new int[1];

        // Invierte verticalmente la imagen.
        STBImage.stbi_set_flip_vertically_on_load(true);

        // Cargar imagen en memoria.
        ByteBuffer imagen =
            STBImage.stbi_load(path, w, h, canales, 4);

        // Validar carga.
        if (imagen == null) {

            throw new RuntimeException(
                "No se pudo cargar: " + path
            );
        }

        // Generar textura OpenGL.
        textureId = GL11.glGenTextures();

        // Activar textura.
        GL11.glBindTexture(
            GL11.GL_TEXTURE_2D,
            textureId
        );

        // Configuración de repetición horizontal.
        GL11.glTexParameteri(
            GL11.GL_TEXTURE_2D,
            GL11.GL_TEXTURE_WRAP_S,
            GL11.GL_REPEAT
        );

        // Configuración de repetición vertical.
        GL11.glTexParameteri(
            GL11.GL_TEXTURE_2D,
            GL11.GL_TEXTURE_WRAP_T,
            GL11.GL_REPEAT
        );

        // Filtrado cuando se reduce la textura.
        GL11.glTexParameteri(
            GL11.GL_TEXTURE_2D,
            GL11.GL_TEXTURE_MIN_FILTER,
            GL11.GL_LINEAR
        );

        // Filtrado cuando se amplía la textura.
        GL11.glTexParameteri(
            GL11.GL_TEXTURE_2D,
            GL11.GL_TEXTURE_MAG_FILTER,
            GL11.GL_LINEAR
        );

        // Enviar textura a OpenGL.
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            w[0],
            h[0],
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            imagen
        );

        // Liberar memoria de imagen.
        STBImage.stbi_image_free(imagen);
    }

    /**
     * Crea las primeras nubes del escenario.
     */
    private void iniciarNubes() {

        nubes.add(new Nube(0.5f, 0.7f, 0.4f));

        nubes.add(new Nube(-0.2f, 0.6f, 0.3f));

        nubes.add(new Nube(1.5f, 0.8f, 0.5f));
    }

    /**
     * Actualiza el movimiento de las nubes.
     *
     * @param dt         Delta time.
     * @param velocidad  Velocidad principal del escenario.
     */
    public void actualizar(float dt, float velocidad) {

        // Mover todas las nubes hacia la izquierda.
        for (Nube n : nubes) {

            n.x -= (velocidad * 0.3f) * dt;
        }

        // Eliminar nubes que salieron de pantalla.
        nubes.removeIf(n -> n.x < -1.8f);

        // Actualizar temporizador.
        timerNube += dt;

        // Generar nueva nube cada 5 segundos.
        if (timerNube > 5.0f) {

            timerNube = 0f;

            // Posición vertical aleatoria.
            float yRandom =
                -0.2f + random.nextFloat() * 1.0f;

            // Escala aleatoria.
            float escalaRandom =
                0.3f + random.nextFloat() * 0.3f;

            // Crear nueva nube.
            nubes.add(
                new Nube(
                    1.5f,
                    yRandom,
                    escalaRandom
                )
            );
        }
    }

    /**
     * Dibuja todas las nubes.
     *
     * @param vao                    VAO del quad base.
     * @param uUsarTexturaLocation   Uniform del shader.
     */
    public void render(
        int vao,
        int uUsarTexturaLocation
    ) {

        // Activar transparencia.
        GL11.glEnable(GL11.GL_BLEND);

        // Configurar mezcla alfa.
        GL11.glBlendFunc(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        );

        // Indicar al shader que usará textura.
        GL20.glUniform1i(
            uUsarTexturaLocation,
            1
        );

        // Activar textura de nube.
        GL11.glBindTexture(
            GL11.GL_TEXTURE_2D,
            textureId
        );

        // Activar VAO.
        GL30.glBindVertexArray(vao);

        // Dibujar cada nube.
        for (Nube n : nubes) {

            dibujarRect(
                n.x,
                n.y,
                n.escala * 1.5f,
                n.escala
            );
        }

        // Desactivar uso de textura.
        GL20.glUniform1i(
            uUsarTexturaLocation,
            0
        );

        // Desactivar blending.
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Dibuja un quad rectangular.
     *
     * @param x       Posición horizontal.
     * @param y       Posición vertical.
     * @param ancho   Escala horizontal.
     * @param alto    Escala vertical.
     */
    private void dibujarRect(
        float x,
        float y,
        float ancho,
        float alto
    ) {

        // Enviar posición al shader.
        GL20.glUniform2f(
            GL20.glGetUniformLocation(
                GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                "uOffset"
            ),
            x,
            y
        );

        // Enviar escala al shader.
        GL20.glUniform2f(
            GL20.glGetUniformLocation(
                GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                "uScale"
            ),
            ancho,
            alto
        );

        // Dibujar quad.
        GL11.glDrawArrays(
            GL11.GL_TRIANGLES,
            0,
            6
        );
    }
}