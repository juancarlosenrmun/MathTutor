package com.mathtutor.application.controller;

import com.mathtutor.application.security.AccountLifecycle;
import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.persistence.repository.PersonalDataStore;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** @brief Derechos sobre los datos de la cuenta autenticada (UC-05). */
@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {
    private final PersonalDataStore data;
    private final AccountLifecycle accounts;

    public PrivacyController(PersonalDataStore data, AccountLifecycle accounts) {
        this.data = data;
        this.accounts = accounts;
    }

    /** @brief Descarga una instantánea en JSON sin permitir su almacenamiento en caché. */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> export(@AuthenticationPrincipal AuthenticatedUser user) {
        String snapshot = data.export(user.id());
        if (snapshot == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no disponible");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mathtutor-datos.json").body(snapshot);
    }

    /** @brief Suprime la cuenta autenticada. La protección CSRF se aplica en el filtro. */
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void erase(@AuthenticationPrincipal AuthenticatedUser user) {
        accounts.erase(user.id());
    }
}
