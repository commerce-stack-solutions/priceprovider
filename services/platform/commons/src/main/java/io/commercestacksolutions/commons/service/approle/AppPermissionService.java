package io.commercestacksolutions.commons.service.approle;

import io.commercestacksolutions.commons.dataaccess.approle.entity.AppPermissionEntity;

import java.util.List;

public interface AppPermissionService {

    List<? extends AppPermissionEntity> getAllAppPermissions();

    AppPermissionEntity createPermission(String name, String description);
}
