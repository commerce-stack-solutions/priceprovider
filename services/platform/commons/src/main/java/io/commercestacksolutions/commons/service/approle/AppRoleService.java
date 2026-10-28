package io.commercestacksolutions.commons.service.approle;

import io.commercestacksolutions.commons.dataaccess.approle.entity.CommonAppPermission;
import io.commercestacksolutions.commons.dataaccess.approle.entity.CommonAppRole;

import java.util.List;
import java.util.Set;

public interface AppRoleService {

    List<? extends CommonAppRole> getAllAppRoles();

    CommonAppRole getAppRoleWithPermissionsByName(String name);

    CommonAppRole createRole(String name, String description, Set<? extends CommonAppPermission> permissions);
}
