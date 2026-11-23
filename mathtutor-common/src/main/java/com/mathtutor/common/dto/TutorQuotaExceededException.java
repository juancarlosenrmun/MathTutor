package com.mathtutor.common.dto;

/** @brief El presupuesto diario no alcanza para una nueva petición al generador. */
public final class TutorQuotaExceededException extends RuntimeException {
    public TutorQuotaExceededException() {
        super("Has alcanzado tu cuota diaria del tutor. Puedes volver a intentarlo mañana (UTC).");
    }
}
