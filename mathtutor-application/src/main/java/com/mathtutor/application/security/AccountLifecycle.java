package com.mathtutor.application.security;

import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.PersonalDataStore;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** @brief Aplica la supresión propia y protege la continuidad de la administración. */
@Service
public class AccountLifecycle {
    private final UserRepository users;
    private final PersonalDataStore data;

    public AccountLifecycle(UserRepository users, PersonalDataStore data) {
        this.users = users;
        this.data = data;
    }

    /** @brief Suprime la cuenta y sus datos en una sola transacción. */
    @Transactional
    public void erase(long id) {
        data.lockAccountChanges();
        User user = users.findByIdForUpdate(id).filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no disponible"));
        requireOtherAdministrator(user);
        data.eraseRelated(id);
        users.delete(user);
    }

    /** @brief Conserva al menos un administrador activo para gestionar el sistema. */
    void requireOtherAdministrator(User user) {
        if (user.isActive() && user.getRole() == User.Role.ADMIN && users.countByActiveTrueAndRole(User.Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar un administrador activo.");
        }
    }
}
