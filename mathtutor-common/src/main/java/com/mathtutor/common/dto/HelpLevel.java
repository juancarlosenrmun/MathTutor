package com.mathtutor.common.dto;

/**
 * Nivel de ayuda que el tutor puede proporcionar.
 * Ordenados de menor a mayor revelación.
 */
public enum HelpLevel {
    CONCEPTUAL,       // Explicación conceptual general
    PROCEDIMENTAL,    // Guía paso a paso del procedimiento
    EJEMPLO_ANALOGO,  // Ejemplo similar resuelto
    REVELACION        // Revelación de la solución
}