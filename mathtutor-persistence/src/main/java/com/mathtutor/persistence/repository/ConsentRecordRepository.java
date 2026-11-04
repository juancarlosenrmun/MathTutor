package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.ConsentRecord;
import com.mathtutor.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para entidad ConsentRecord.
 */
@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    List<ConsentRecord> findByUser(User user);

    List<ConsentRecord> findByUserAndType(User user, ConsentRecord.ConsentType type);
}