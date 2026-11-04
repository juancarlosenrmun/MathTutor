package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.ConsentRecord;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para ConsentRecordRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class ConsentRecordRepositoryTest {

    @Autowired
    private ConsentRecordRepository consentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindConsentRecordsByUser() {
        // Given
        User user = User.builder().email("consent@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        ConsentRecord consent1 = ConsentRecord.builder()
                .type(ConsentRecord.ConsentType.PRIVACIDAD)
                .accepted(true)
                .policyVersion("1.0")
                .user(user)
                .build();
        ConsentRecord consent2 = ConsentRecord.builder()
                .type(ConsentRecord.ConsentType.COOKIES_TECNICAS)
                .accepted(true)
                .policyVersion("1.0")
                .user(user)
                .build();
        consentRepository.save(consent1);
        consentRepository.save(consent2);

        // When
        List<ConsentRecord> consents = consentRepository.findByUser(user);

        // Then
        assertThat(consents).hasSize(2);
    }

    @Test
    void shouldFindByUserAndType() {
        // Given
        User user = User.builder().email("consent2@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        ConsentRecord consent = ConsentRecord.builder()
                .type(ConsentRecord.ConsentType.PRIVACIDAD)
                .accepted(true)
                .policyVersion("2.0")
                .user(user)
                .build();
        consentRepository.save(consent);

        // When
        List<ConsentRecord> consents = consentRepository.findByUserAndType(user, ConsentRecord.ConsentType.PRIVACIDAD);

        // Then
        assertThat(consents).hasSize(1);
        assertThat(consents.get(0).getPolicyVersion()).isEqualTo("2.0");
    }

    @Test
    void shouldReturnEmptyWhenTypeNotExists() {
        // Given
        User user = User.builder().email("consent3@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        // When
        List<ConsentRecord> consents = consentRepository.findByUserAndType(user, ConsentRecord.ConsentType.COOKIES_NO_ESENCIALES);

        // Then
        assertThat(consents).isEmpty();
    }
}
