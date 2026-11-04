package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.UsageQuota;
import com.mathtutor.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repositorio para entidad UsageQuota.
 */
@Repository
public interface UsageQuotaRepository extends JpaRepository<UsageQuota, Long> {

    Optional<UsageQuota> findByUserAndDay(User user, LocalDate day);

    void deleteByDayBefore(LocalDate day);
}