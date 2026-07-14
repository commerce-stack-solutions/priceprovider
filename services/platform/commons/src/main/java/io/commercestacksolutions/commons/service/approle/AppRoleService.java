package io.commercestacksolutions.commons.service.approle;

import io.commercestacksolutions.commons.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.commons.dataaccess.approle.entity.AppRoleEntity;

import java.util.List;
import java.util.Set;

public interface AppRoleService {

    List<? extends AppRoleEntity> getAllAppRoles();

    AppRoleEntity getAppRoleWithPermissionsByName(String name);

    AppRoleEntity createRole(String name, String description, Set<? extends AppPermissionEntity> permissions);
}
