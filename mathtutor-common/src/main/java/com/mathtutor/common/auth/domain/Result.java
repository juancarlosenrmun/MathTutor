package com.mathtutor.common.auth.domain;

/**
 * Tipo resultado para casos de uso.
 * Evita excepciones para flujo de control normal.
 *
 * @param <T> tipo de valor en caso de éxito
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    /**
     * Resultado exitoso con valor.
     *
     * @param <T> tipo de valor
     */
    record Success<T>(T value) implements Result<T> {}

    /**
     * Resultado fallido con código y mensaje de error.
     *
     * @param <T> tipo de valor (nunca presente)
     */
    record Failure<T>(ErrorCode code, String message) implements Result<T> {}

    /**
     * Códigos de error estandarizados para autenticación.
     */
    enum ErrorCode {
        /** Email ya registrado */
        EMAIL_ALREADY_EXISTS,
        /** Email no encontrado */
        EMAIL_NOT_FOUND,
        /** Contraseña incorrecta */
        INVALID_PASSWORD,
        /** Usuario inactivo */
        USER_INACTIVE,
        /** Validación de entrada fallida */
        VALIDATION_ERROR,
        /** Error interno */
        INTERNAL_ERROR,
        /** Token inválido o expirado */
        INVALID_TOKEN,
        /** Token de refresco revocado */
        REFRESH_TOKEN_REVOKED
    }

    /**
     * Crea un resultado exitoso.
     *
     * @param value valor de éxito
     * @param <T>   tipo de valor
     * @return resultado exitoso
     */
    static <T> Result<T> ok(T value) {
        return new Success<>(value);
    }

    /**
     * Crea un resultado fallido.
     *
     * @param code    código de error
     * @param message mensaje de error
     * @param <T>     tipo de valor
     * @return resultado fallido
     */
    static <T> Result<T> fail(ErrorCode code, String message) {
        return new Failure<>(code, message);
    }

    /**
     * Verifica si el resultado es exitoso.
     *
     * @return true si éxito
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Verifica si el resultado es fallido.
     *
     * @return true si fallo
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Obtiene el valor si es éxito, lanza excepción si es fallo.
     *
     * @return valor de éxito
     * @throws IllegalStateException si es fallo
     */
    default T getOrThrow() {
        if (this instanceof Success<T> s) {
            return s.value();
        }
        Failure<T> f = (Failure<T>) this;
        throw new IllegalStateException(f.code() + ": " + f.message());
    }
}