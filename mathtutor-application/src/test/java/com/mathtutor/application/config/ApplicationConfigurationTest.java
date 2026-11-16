package com.mathtutor.application.config;

import com.mathtutor.common.auth.domain.User;
import com.mathtutor.common.auth.port.UserRepositoryPort;
import com.mathtutor.common.port.PasswordEncoderPort;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    @Mock private UserRepositoryPort users;
    @Mock private PasswordEncoderPort passwordEncoder;

    @Test
    void loadsAnActiveUserWithItsCurrentRole() {
        var user = User.withId(7L, "alumno@example.test", "hash", User.Role.ALUMNO, true, null, null);
        when(users.findActiveByEmail("alumno@example.test")).thenReturn(Optional.of(user));

        var details = new UserDetailsServiceImpl(users).loadUserByUsername("alumno@example.test");

        assertThat(details.getUsername()).isEqualTo("alumno@example.test");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ALUMNO");
    }

    @Test
    void rejectsAnUnknownOrInactiveUser() {
        when(users.findActiveByEmail("missing@example.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UserDetailsServiceImpl(users).loadUserByUsername("missing@example.test"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessage("User not found: missing@example.test");
    }

    @Test
    void mapsAnInactiveUserAsDisabledIfTheRepositoryReturnsIt() {
        var user = User.withId(8L, "inactive@example.test", "hash", User.Role.PROFESOR, false, null, null);
        when(users.findActiveByEmail("inactive@example.test")).thenReturn(Optional.of(user));

        var details = new UserDetailsServiceImpl(users).loadUserByUsername("inactive@example.test");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_PROFESOR");
    }

    @Test
    void delegatesPasswordEncodingAndMatchingToTheDomainPort() {
        when(passwordEncoder.encode("raw")).thenReturn("encoded");
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);
        var adapter = new SpringSecurityPasswordEncoderAdapter(passwordEncoder);

        assertThat(adapter.encode("raw")).isEqualTo("encoded");
        assertThat(adapter.matches("raw", "encoded")).isTrue();
        verify(passwordEncoder).encode("raw");
        verify(passwordEncoder).matches("raw", "encoded");
    }

    @Test
    void formatsValidationAndIllegalArgumentErrorsAsTheApiContract() {
        var handler = new GlobalExceptionHandler();
        var bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        var validation = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        when(validation.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new FieldError("request", "email", "email inválido")));

        var invalid = handler.handleValidationExceptions(validation);
        var constraint = handler.handleConstraintViolation(new ConstraintViolationException("límite", java.util.Set.of()));
        var illegal = handler.handleIllegalArgument(new IllegalArgumentException("dato inválido"));

        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).isEqualTo(new GlobalExceptionHandler.ApiResponse<>(false, null,
                "Error de validación: {email=email inválido}"));
        assertThat(constraint.getBody().error()).contains("límite");
        assertThat(illegal.getBody()).isEqualTo(new GlobalExceptionHandler.ApiResponse<>(false, null, "dato inválido"));
    }
}
