package com.mathtutor.common.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.List;

/**
 * Eventos que se envían al cliente vía SSE durante un turno de tutoría.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TurnEvent.Token.class, name = "TOKEN"),
    @JsonSubTypes.Type(value = TurnEvent.Done.class, name = "DONE"),
    @JsonSubTypes.Type(value = TurnEvent.Error.class, name = "ERROR"),
    @JsonSubTypes.Type(value = TurnEvent.GraphicSpec.class, name = "GRAPHIC_SPEC"),
    @JsonSubTypes.Type(value = TurnEvent.HelpLevelChanged.class, name = "HELP_LEVEL_CHANGED"),
    @JsonSubTypes.Type(value = TurnEvent.Diagnosis.class, name = "DIAGNOSIS")
})
public sealed interface TurnEvent permits TurnEvent.Token, TurnEvent.Done, TurnEvent.Error, TurnEvent.GraphicSpec, TurnEvent.HelpLevelChanged, TurnEvent.Diagnosis {

    record Token(String content, Instant timestamp) implements TurnEvent {}

    record Done(String fullResponse, int tokensIn, int tokensOut, long latencyMs, Instant timestamp) implements TurnEvent {}

    record Error(String code, String message, Instant timestamp) implements TurnEvent {}

    record GraphicSpec(com.mathtutor.common.dto.GraphicSpec spec, Instant timestamp) implements TurnEvent {}

    record HelpLevelChanged(HelpLevel level, Instant timestamp) implements TurnEvent {}

    record Diagnosis(ErrorType errorType, String hint, Instant timestamp) implements TurnEvent {}
}