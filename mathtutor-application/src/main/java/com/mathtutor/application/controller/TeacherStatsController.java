package com.mathtutor.application.controller;

import com.mathtutor.persistence.repository.ExerciseStatistics;
import com.mathtutor.persistence.repository.AdminUsageRepository;
import com.mathtutor.application.security.AdminUsage;
import com.mathtutor.auth.filter.JwtAuthenticationFilter.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** @brief Ofrece al profesorado resultados agregados por módulo y tema (UC-10). */
@RestController
@RequestMapping("/api/teacher/statistics")
@PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
public class TeacherStatsController {
    private final ExerciseStatistics statistics;

    private final AdminUsage usage;

    public TeacherStatsController(ExerciseStatistics statistics, AdminUsage usage) {
        this.usage = usage;
        this.statistics = statistics;
    }

    @GetMapping("/students")
    public ResponseEntity<List<AdminUsage.Student>> students(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usage.students(page, size));
    }

    @GetMapping("/students/{id}/sessions")
    public ResponseEntity<AdminUsage.ResultPage<AdminUsageRepository.SessionUsage>> sessions(
            @AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(usage.studentSessions(actor.id(), id, page, size));
    }

    @GetMapping("/usage/topics")
    public ResponseEntity<AdminUsage.ResultPage<AdminUsageRepository.TopicUsage>> topics(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usage.topics(page, size));
    }

    /**
     * @brief Consulta los conteos sin identificadores ni contenido del alumnado.
     * @param module Código opcional del módulo.
     * @return Una fila por módulo y tema con ejercicios registrados.
     */
    @GetMapping
    public List<ExerciseStatistics.Summary> statistics(@RequestParam(required = false) String module) {
        return statistics.summarize(module, Instant.now());
    }
}
