package io.commercestacksolutions.commons.dataaccess.approle.entity;

import java.util.Set;

public interface AppRoleEntity {

    String getName();

    Set<? extends AppPermissionEntity> getPermissionRefs();
}
