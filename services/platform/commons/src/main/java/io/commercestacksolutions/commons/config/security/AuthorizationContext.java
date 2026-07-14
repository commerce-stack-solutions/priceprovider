package io.commercestacksolutions.commons.config.security;

import io.commercestacksolutions.commons.dataaccess.approle.entity.AppPermissionEntity;

import java.util.Set;

public interface AuthorizationContext {

    Set<? extends AppPermissionEntity> getCurrentPermissions();

    boolean isBootstrapModeEnabled();

    void activateBootstrapMode();

    void deactivateBootstrapMode();
}
