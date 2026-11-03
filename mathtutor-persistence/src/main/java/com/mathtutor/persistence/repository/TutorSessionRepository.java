package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.TutorSession;
import com.mathtutor.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad TutorSession.
 */
@Repository
public interface TutorSessionRepository extends JpaRepository<TutorSession, Long> {

    List<TutorSession> findByUser(User user);

    List<TutorSession> findByUserAndStatus(User user, TutorSession.SessionStatus status);

    Optional<TutorSession> findByUserAndStatusAndModule(User user, TutorSession.SessionStatus status, Module module);

    @Query("SELECT s FROM TutorSession s WHERE s.user = :user AND s.status = :status ORDER BY s.lastActivityAt DESC")
    List<TutorSession> findByUserAndStatusOrderByLastActivityDesc(@Param("user") User user, @Param("status") TutorSession.SessionStatus status);

    Page<TutorSession> findByUser(User user, Pageable pageable);

    List<TutorSession> findByStatus(TutorSession.SessionStatus status);

    List<TutorSession> findByLastActivityAtBefore(Instant threshold);
}