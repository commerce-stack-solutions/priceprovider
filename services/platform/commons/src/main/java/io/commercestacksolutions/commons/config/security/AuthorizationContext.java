package io.commercestacksolutions.commons.config.security;

import io.commercestacksolutions.commons.dataaccess.approle.entity.CommonAppPermission;

import java.util.Set;

public interface AuthorizationContext {

    Set<? extends CommonAppPermission> getCurrentPermissions();

    boolean isBootstrapModeEnabled();

    void activateBootstrapMode();

    void deactivateBootstrapMode();
}
