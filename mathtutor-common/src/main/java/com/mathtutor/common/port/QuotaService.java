package com.mathtutor.common.port;

/**
 * Puerto para gestión de cuota de tokens por usuario y día (RNF-09).
 */
public interface QuotaService {

    /**
     * Verifica si el usuario tiene cuota disponible.
     *
     * @param userId ID del usuario
     * @return true si tiene cuota
     */
    boolean check(String userId);

    /**
     * Registra el consumo de tokens.
     *
     * @param userId  ID del usuario
     * @param tokens  tokens consumidos
     */
    void register(String userId, int tokens);

    /**
     * Obtiene la cuota restante del día.
     *
     * @param userId ID del usuario
     * @return tokens restantes
     */
    long remaining(String userId);

    /**
     * Obtiene el límite diario de tokens.
     *
     * @param userId ID del usuario
     * @return límite diario
     */
    long limit(String userId);
}