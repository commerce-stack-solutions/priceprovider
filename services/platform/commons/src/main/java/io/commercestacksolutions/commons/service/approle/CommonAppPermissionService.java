package io.commercestacksolutions.commons.service.approle;

import io.commercestacksolutions.commons.dataaccess.approle.entity.CommonAppPermission;

import java.util.List;

public interface CommonAppPermissionService {

    List<? extends CommonAppPermission> getAllAppPermissions();

    CommonAppPermission createPermission(String name, String description);
}
