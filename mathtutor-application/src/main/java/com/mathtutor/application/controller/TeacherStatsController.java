package com.mathtutor.application.controller;

import com.mathtutor.persistence.repository.ExerciseStatistics;
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

    public TeacherStatsController(ExerciseStatistics statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    public List<ExerciseStatistics.Summary> statistics(@RequestParam(required = false) String module) {
        return statistics.summarize(module, Instant.now());
    }
}
