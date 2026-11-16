package com.mathtutor.auth;

import com.mathtutor.common.dto.Role;
import com.mathtutor.common.port.AccessPolicy.Action;
import com.mathtutor.common.port.AccessPolicy.Resource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Tabla de decisión completa: permisos explícitos y denegación por defecto. */
class RoleAccessPolicyTest {
    @Test
    void appliesDecisionTable() {
        var policy = new RoleAccessPolicy();
        for (var role : Role.values()) {
            for (var action : Action.values()) {
                for (var resource : Resource.values()) {
                    boolean expected = role == Role.ADMIN || action == Action.READ && (
                            resource == Resource.MODULE || resource == Resource.EXERCISE
                            || role == Role.ALUMNO && resource == Resource.SESSION
                            || role == Role.PROFESOR && resource == Resource.STATS);
                    assertThat(policy.can(role, action, resource))
                            .as("%s %s %s", role, action, resource).isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void rejectsIncompleteDecisions() {
        var policy = new RoleAccessPolicy();
        assertThat(policy.can(null, Action.READ, Resource.SESSION)).isFalse();
        assertThat(policy.can(Role.ADMIN, null, Resource.SESSION)).isFalse();
        assertThat(policy.can(Role.ADMIN, Action.READ, null)).isFalse();
    }
}
