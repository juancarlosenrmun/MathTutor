package com.mathtutor.auth.adapter;

import com.mathtutor.common.port.PasswordEncoderPort;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;

/**
 * Implementación de PasswordEncoderPort usando Argon2id (Argon2 v1.3).
 *
 * <p><strong>Elección de Argon2id:</strong> Argon2id es la variante híbrida recomendada por el
 * IRTF CFRG (RFC 9106) y OWASP, ya que combina la resistencia a ataques de tradeoff tiempo-memoria
 * de Argon2i (acceso a memoria dependiente de la contraseña) con la resistencia a ataques de
 * canal lateral de Argon2d (acceso a memoria dependiente de los datos). Es la opción por defecto
 * para hashing de contraseñas en aplicaciones modernas.
 *
 * <p><strong>Parámetros configurados (OWASP Cheat Sheet / ASVS L1 V2.2.1):</strong>
 * <ul>
 *   <li><strong>Memoria (memory cost):</strong> 65,536 KiB = 64 MB. OWASP recomienda ≥ 64 MB para
 *       aplicaciones web en 2024. Este valor fuerza al atacante a usar 64 MB por intento de
 *       cracking, limitando drásticamente la paralelización en GPU/ASIC.</li>
 *   <li><strong>Iteraciones (time cost):</strong> 3 pasadas. OWASP sugiere 3-4 iteraciones para
 *       equilibrio entre seguridad y latencia de login (~100-200 ms en hardware típico).
 *       Cada iteración recorre toda la memoria, multiplicando el costo computacional.</li>
 *   <li><strong>Paralelismo (lanes):</strong> 4 carriles (lanes). Permite aprovechar CPU multinúcleo
 *       legítimo durante el hash, mientras que un atacante con GPU debe multiplicar la memoria
 *       por el número de carriles. Valor típico para servidores de 4-8 núcleos.</li>
 * </ul>
 *
 * <p><strong>Rendimiento estimado (CPU moderna, 2024):</strong> ~80-150 ms por hash/verificación.
 * Latencia aceptable para login interactivo (ASVS L1: < 500 ms).
 *
 * <p><strong>Referencias:</strong>
 * <ul>
 *   <li>OWASP Password Storage Cheat Sheet (2024): Argon2id, memory ≥ 64 MiB, iterations ≥ 3, parallelism ≥ 4</li>
 *   <li>ASVS 4.0.3 L1 V2.2.1: "Verify that passwords are hashed with a suitable adaptive one-way function..."</li>
 *   <li>RFC 9106: Argon2 Memory-Hard Function for Password Hashing and Proof-of-Work Applications</li>
 *   <li>Argon2 specification: memory=64MB, iterations=3, parallelism=4 = perfil "recommended" para apps web</li>
 * </ul>
 */
@Component
public class PasswordEncoderAdapter implements PasswordEncoderPort {

    /**
     * Instancia de Argon2id (variante híbrida recomendada por RFC 9106 y OWASP).
     * Thread-safe: Argon2Factory.create() devuelve una instancia segura para uso concurrente.
     */
    private static final Argon2 ARGON2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    /**
     * Coste de memoria: 65,536 KiB = 64 MiB.
     * OWASP 2024: mínimo 64 MiB para aplicaciones web. Fuerza al atacante a dedicar 64 MB por intento.
     */
    private static final int MEMORY_KIB = 65536; // 64 MB

    /**
     * Coste temporal: 3 iteraciones (pasadas sobre la memoria).
     * OWASP: 3-4 iteraciones. Cada iteración recorre los 64 MB, multiplicando el coste CPU.
     * Latencia objetivo: ~100-200 ms en hardware típico de servidor.
     */
    private static final int ITERATIONS = 3;

    /**
     * Paralelismo: 4 carriles (lanes) independientes.
     * Permite hash paralelo en CPU legítima (4 hilos). Atacante con GPU debe multiplicar memoria × lanes.
     * Valor típico para servidores de 4-8 núcleos.
     */
    private static final int PARALLELISM = 4;

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (rawPassword.length() == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        return ARGON2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }

        try {
            return ARGON2.verify(encodedPassword, rawPassword.toString());
        } catch (IllegalArgumentException e) {
            // Hash inválido o formato incorrecto
            return false;
        }
    }
}