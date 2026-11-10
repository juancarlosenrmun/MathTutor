package com.mathtutor.common.auth.port;

import java.time.Duration;

/**
 * Puerto para limitación de tasa (rate limiting) de intentos de login.
 * Previene ataques de fuerza bruta y enumeraación de usuarios.
 */
public interface RateLimitPort {

    /**
     * Intenta consumir un intento para la clave dada.
     *
     * @param key          clave única (ej: "login:ip:192.168.1.1" o "login:email:user@example.com")
     * @param maxAttempts  máximo de intentos permitidos en la ventana
     * @param window       ventana de tiempo
     * @return true si el intento se permite, false si se excede el límite
     */
    boolean tryConsume(String key, int maxAttempts, Duration window);

    /**
     * Reinicia el contador para una clave (ej: tras login exitoso).
     *
     * @param key clave a reiniciar
     */
    void reset(String key);
}