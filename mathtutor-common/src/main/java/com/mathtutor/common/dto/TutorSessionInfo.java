package com.mathtutor.common.dto;

/** @brief Instantánea sin entidades JPA ni información personal. */
public record TutorSessionInfo(Long id, String moduleCode, String status) {}
