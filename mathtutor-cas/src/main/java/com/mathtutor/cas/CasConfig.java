package com.mathtutor.cas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del módulo CAS.
 * Registra los beans de SymjaCasEngine y LatexToSymjaConverter.
 */
@Configuration
public class CasConfig {

    @Bean
    public LatexToSymjaConverter latexToSymjaConverter() {
        return new LatexToSymjaConverter();
    }

    @Bean
    public SymjaCasEngine symjaCasEngine(LatexToSymjaConverter converter) {
        return new SymjaCasEngine(converter);
    }
}