package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.UsageQuota;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para UsageQuotaRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class UsageQuotaRepositoryTest {

    @Autowired
    private UsageQuotaRepository quotaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindQuotaByUserAndDay() {
        // Given
        User user = User.builder().email("quota@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        UsageQuota quota = UsageQuota.builder()
                .day(LocalDate.now())
                .tokensUsed(100)
                .tokenLimit(10000)
                .user(user)
                .build();

        // When
        UsageQuota saved = quotaRepository.save(quota);
        Optional<UsageQuota> found = quotaRepository.findByUserAndDay(user, LocalDate.now());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTokensUsed()).isEqualTo(100);
        assertThat(found.get().getTokenLimit()).isEqualTo(10000);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldReturnEmptyWhenQuotaNotExists() {
        // Given
        User user = User.builder().email("quota2@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        // When
        Optional<UsageQuota> found = quotaRepository.findByUserAndDay(user, LocalDate.now());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldDeleteByDayBefore() {
        // Given
        User user = User.builder().email("quota3@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        UsageQuota oldQuota = UsageQuota.builder().day(LocalDate.now().minusDays(10)).tokensUsed(500).tokenLimit(10000).user(user).build();
        UsageQuota newQuota = UsageQuota.builder().day(LocalDate.now()).tokensUsed(100).tokenLimit(10000).user(user).build();
        quotaRepository.save(oldQuota);
        quotaRepository.save(newQuota);

        // When
        quotaRepository.deleteByDayBefore(LocalDate.now().minusDays(5));

        // Then
        assertThat(quotaRepository.findByUserAndDay(user, LocalDate.now().minusDays(10))).isEmpty();
        assertThat(quotaRepository.findByUserAndDay(user, LocalDate.now())).isPresent();
    }

    @Test
    void shouldEnforceUniqueConstraintUserAndDay() {
        // Given
        User user = User.builder().email("quota4@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        UsageQuota quota1 = UsageQuota.builder().day(LocalDate.now()).tokensUsed(100).tokenLimit(10000).user(user).build();
        quotaRepository.save(quota1);

        UsageQuota quota2 = UsageQuota.builder().day(LocalDate.now()).tokensUsed(200).tokenLimit(10000).user(user).build();

        // When/Then - Should throw exception on duplicate
        try {
            quotaRepository.save(quota2);
            assertThat(false).as("Should have thrown exception").isTrue();
        } catch (Exception e) {
            // Expected
        }
    }
}
