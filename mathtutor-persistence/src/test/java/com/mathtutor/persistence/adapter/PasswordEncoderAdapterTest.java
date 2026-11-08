package com.mathtutor.persistence.adapter;

import com.mathtutor.common.port.PasswordEncoderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests para PasswordEncoderAdapter con Argon2id (TDD - Red phase primero).
 */
@DisplayName("PasswordEncoderAdapter (Argon2id)")
class PasswordEncoderAdapterTest {

    private final PasswordEncoderPort encoder = new PasswordEncoderAdapter();

    @Nested
    @DisplayName("encode()")
    class Encode {

        @Test
        @DisplayName("Debe generar hash Argon2id válido para contraseña simple")
        void shouldGenerateValidArgon2idHashForSimplePassword() {
            // When
            String hash = encoder.encode("Pass123!");

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash).startsWith("$argon2id$");
            // Formato: $argon2id$v=19$m=65536,t=3,p=4$salt$hash
            assertThat(hash).contains("m=65536,t=3,p=4");
        }

        @Test
        @DisplayName("Debe generar hash diferente cada vez (salt único)")
        void shouldGenerateDifferentHashEachTime() {
            // When
            String hash1 = encoder.encode("Pass123!");
            String hash2 = encoder.encode("Pass123!");

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
            // Pero ambos deben ser válidos
            assertThat(encoder.matches("Pass123!", hash1)).isTrue();
            assertThat(encoder.matches("Pass123!", hash2)).isTrue();
        }

        @Test
        @DisplayName("Debe manejar contraseña con caracteres Unicode")
        void shouldHandleUnicodePassword() {
            // When
            String hash = encoder.encode("Pässwörd123!🔐");

            // Then
            assertThat(hash).startsWith("$argon2id$");
            assertThat(encoder.matches("Pässwörd123!🔐", hash)).isTrue();
        }

        @Test
        @DisplayName("Debe manejar contraseña muy larga")
        void shouldHandleVeryLongPassword() {
            // Given - contraseña de 1000 caracteres
            String longPassword = "A".repeat(500) + "a".repeat(500) + "1!";

            // When
            String hash = encoder.encode(longPassword);

            // Then
            assertThat(hash).startsWith("$argon2id$");
            assertThat(encoder.matches(longPassword, hash)).isTrue();
        }

        @Test
        @DisplayName("Debe lanzar excepción para contraseña null")
        void shouldThrowForNullPassword() {
            // When / Then
            assertThatThrownBy(() -> encoder.encode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password cannot be null");
        }

        @Test
        @DisplayName("Debe lanzar excepción para contraseña vacía")
        void shouldThrowForEmptyPassword() {
            // When / Then
            assertThatThrownBy(() -> encoder.encode(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password cannot be empty");
        }
    }

    @Nested
    @DisplayName("matches()")
    class Matches {

        @Test
        @DisplayName("Debe retornar true para contraseña correcta")
        void shouldReturnTrueForCorrectPassword() {
            // Given
            String password = "Pass123!";
            String hash = encoder.encode(password);

            // When
            boolean result = encoder.matches(password, hash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false para contraseña incorrecta")
        void shouldReturnFalseForIncorrectPassword() {
            // Given
            String hash = encoder.encode("Pass123!");

            // When
            boolean result = encoder.matches("WrongPass123!", hash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para hash null")
        void shouldReturnFalseForNullHash() {
            // When
            boolean result = encoder.matches("Pass123!", null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para hash vacío")
        void shouldReturnFalseForEmptyHash() {
            // When
            boolean result = encoder.matches("Pass123!", "");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para hash inválido")
        void shouldReturnFalseForInvalidHash() {
            // When
            boolean result = encoder.matches("Pass123!", "not-a-valid-hash");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false para hash BCrypt (formato diferente)")
        void shouldReturnFalseForBCryptHash() {
            // Given - hash BCrypt típico
            String bcryptHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

            // When
            boolean result = encoder.matches("Pass123!", bcryptHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe manejar contraseña null en matches")
        void shouldHandleNullPasswordInMatches() {
            // Given
            String hash = encoder.encode("Pass123!");

            // When
            boolean result = encoder.matches(null, hash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Debe ser case-sensitive")
        void shouldBeCaseSensitive() {
            // Given
            String hash = encoder.encode("Pass123!");

            // When / Then
            assertThat(encoder.matches("pass123!", hash)).isFalse();
            assertThat(encoder.matches("PASS123!", hash)).isFalse();
            assertThat(encoder.matches("Pass123!", hash)).isTrue();
        }
    }

    @Nested
    @DisplayName("Parámetros de seguridad Argon2id")
    class SecurityParameters {

        @Test
        @DisplayName("Debe usar memoria 64MB (65536 KB)")
        void shouldUse64MBMemory() {
            // When
            String hash = encoder.encode("Pass123!");

            // Then
            assertThat(hash).contains("m=65536");
        }

        @Test
        @DisplayName("Debe usar 3 iteraciones")
        void shouldUse3Iterations() {
            // When
            String hash = encoder.encode("Pass123!");

            // Then
            assertThat(hash).contains("t=3");
        }

        @Test
        @DisplayName("Debe usar paralelismo 4")
        void shouldUseParallelism4() {
            // When
            String hash = encoder.encode("Pass123!");

            // Then
            assertThat(hash).contains("p=4");
        }

        @Test
        @DisplayName("Debe usar versión 19 (0x13)")
        void shouldUseVersion19() {
            // When
            String hash = encoder.encode("Pass123!");

            // Then
            assertThat(hash).contains("v=19");
        }
    }
}