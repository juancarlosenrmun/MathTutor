package com.mathtutor.application.security;

import com.mathtutor.persistence.entity.AuditLog;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.AdminUsageRepository;
import com.mathtutor.persistence.repository.AuditLogRepository;
import com.mathtutor.persistence.repository.PersonalDataStore;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** @brief Consulta estadísticas de uso y registra el acceso administrativo a sesiones individuales. */
@Service
public class AdminUsage {
    private final UserRepository users;
    private final PersonalDataStore personalData;
    private final AdminUsageRepository usage;
    private final AuditLogRepository audit;

    public AdminUsage(UserRepository users, PersonalDataStore personalData,
                      AdminUsageRepository usage, AuditLogRepository audit) {
        this.users = users;
        this.personalData = personalData;
        this.usage = usage;
        this.audit = audit;
    }

    /** @brief Agrega el uso por tema y comprueba si hay otra página con una fila adicional. */
    @Transactional(readOnly = true)
    public ResultPage<AdminUsageRepository.TopicUsage> topics(int page, int size) {
        var request = request(page, size);
        return new ResultPage<>(usage.topics(request.getOffset(), size + 1), size);
    }

    @Transactional(readOnly = true)
    public List<Student> students(int page, int size) {
        var pagination = request(page, size).withSort(org.springframework.data.domain.Sort.by("id"));
        return users.findByRoleAndAnonymizedAtIsNull(User.Role.ALUMNO, pagination).stream()
                .map(user -> new Student(user.getId(), user.getEmail(), user.isActive())).toList();
    }

    public record Student(long id, String email, boolean active) {}

    /** @brief Mantiene la lectura y su registro de auditoría en la misma transacción que protege el perfil. */
    @Transactional
    public ResultPage<AdminUsageRepository.SessionUsage> sessions(long adminId, long targetId, int page, int size) {
        return sessions(adminId, targetId, page, size, false);
    }

    @Transactional
    public ResultPage<AdminUsageRepository.SessionUsage> studentSessions(long teacherId, long studentId, int page, int size) {
        return sessions(teacherId, studentId, page, size, true);
    }

    private ResultPage<AdminUsageRepository.SessionUsage> sessions(long actorId, long targetId, int page, int size,
                                                                  boolean studentOnly) {
        var request = request(page, size);
        personalData.lockAccountChanges();
        User administrator = users.findByIdForUpdate(actorId).filter(User::isActive)
                .filter(user -> user.getRole() == User.Role.ADMIN || studentOnly && user.getRole() == User.Role.PROFESOR)
                .orElseThrow(() -> new AccessDeniedException("Se requieren permisos de administración."));
        users.findByIdForUpdate(targetId).filter(user -> user.getAnonymizedAt() == null)
                .filter(user -> !studentOnly || user.getRole() == User.Role.ALUMNO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no disponible."));

        var items = usage.sessions(targetId, request.getOffset(), size + 1);
        audit.save(AuditLog.builder().actor(administrator).targetType("USER")
                .targetId(targetId).action("USER_SESSIONS_VIEWED").build());
        return new ResultPage<>(items, size);
    }

    private static PageRequest request(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Paginación no válida (1–100)");
        }
        return PageRequest.of(page, size);
    }

    /** @brief Página estable sin recuento de filas fuera de la ventana consultada. */
    public record ResultPage<T>(List<T> items, boolean hasNext) {
        private ResultPage(List<T> rows, int size) {
            this(List.copyOf(rows.subList(0, Math.min(rows.size(), size))), rows.size() > size);
        }
    }
}
