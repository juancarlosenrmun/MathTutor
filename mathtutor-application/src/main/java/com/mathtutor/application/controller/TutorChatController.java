package com.mathtutor.application.controller;

import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import com.mathtutor.tutor.TutorConversation;
import com.mathtutor.tutor.SessionUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/** @brief POST SSE: conserva Authorization/CSRF y no coloca mensajes ni tokens en URL. */
@RestController @RequestMapping("/api/tutor/sessions")
@PreAuthorize("hasRole('ALUMNO')")
public class TutorChatController {
    private final TutorConversation conversation;
    public TutorChatController(TutorConversation conversation) { this.conversation = conversation; }

    /** @return Eventos TOKEN, DONE o ERROR; nunca expone errores internos del proveedor. */
    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEvent> reply(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable Long id, @Valid @RequestBody Message body) {
        return conversation.reply(user.id(), id, body.message())
                .map(token -> new ChatEvent("TOKEN", token))
                .concatWithValues(new ChatEvent("DONE", ""))
                .onErrorReturn(new ChatEvent("ERROR", "No se pudo completar la respuesta. Inténtalo de nuevo."));
    }

    @ExceptionHandler(SessionUnavailableException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public String unavailable() { return "Sesión no disponible"; }

    public record Message(@NotBlank @Size(max = 4000) String message) {}
    public record ChatEvent(String type, String content) {}
}
