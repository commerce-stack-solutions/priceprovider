package io.commercestacksolutions.commons.dataaccess.approle.entity;

import java.util.Set;

public interface CommonAppRole {

    String getName();

    Set<? extends CommonAppPermission> getPermissionRefs();
}
