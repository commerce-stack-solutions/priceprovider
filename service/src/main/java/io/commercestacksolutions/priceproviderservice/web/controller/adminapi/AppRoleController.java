package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.approle.AppRoleFacade;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/api/app-roles")
@Tag(name = "AppRoles", description = "Application role management API")
public class AppRoleController {

    private final AppRoleFacade appRoleFacade;

    @Autowired
    public AppRoleController(AppRoleFacade appRoleFacade) {
        this.appRoleFacade = appRoleFacade;
    }

    @Operation(summary = "Get list of app roles", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved app roles",
                    content = @Content(schema = @Schema(implementation = AppRoleListRestEntity.class)))
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'read')")
    @GetMapping
    public AppRoleListRestEntity getAppRoles(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,
            @RequestParam(value = "sort-direction", required = false) String sortDirection,
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return appRoleFacade.getAppRoles(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(summary = "Get app role by id", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved app role"),
            @ApiResponse(responseCode = "404", description = "App role not found")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'read')")
    @GetMapping("/{id}")
    public AppRoleRestEntity getAppRole(
            @PathVariable("id") Long id,
            @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        return appRoleFacade.getAppRole(id, expand);
    }

    @Operation(summary = "Get app role by name", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved app role"),
            @ApiResponse(responseCode = "404", description = "App role not found")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/by-name/{name}")
    public AppRoleRestEntity getAppRoleByName(
            @PathVariable("name") String name,
            @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);
        return appRoleFacade.getAppRoleByName(decodedName, expand);
    }

    @Operation(summary = "Get meta information for app roles")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return appRoleFacade.getMeta();
    }

    @Operation(summary = "Partially update app role", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully patched app role"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "App role not found")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'write')")
    @PatchMapping("/{id}")
    public AppRoleRestEntity patch(
            @PathVariable("id") Long id,
            @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return appRoleFacade.patch(id, patch);
    }

    @Operation(summary = "Create or update app role", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully created or updated app role"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'write')")
    @PutMapping("/{id}")
    public AppRoleRestEntity createOrRecreate(
            @PathVariable("id") Long id,
            @RequestBody AppRoleRestEntity restEntity
    ) throws DataMappingException, EntityValidationException, NotFoundException {
        return appRoleFacade.createOrRecreate(id, restEntity);
    }

    @Operation(summary = "Create new app role", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully created app role"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "App role with the same ID already exists")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'write')")
    @PostMapping("/create")
    public AppRoleRestEntity create(
            @RequestBody AppRoleRestEntity restEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return appRoleFacade.create(restEntity);
    }

    @Operation(summary = "Delete app role", responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted app role"),
            @ApiResponse(responseCode = "404", description = "App role not found")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            appRoleFacade.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Bulk delete app roles", responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted app roles"),
            @ApiResponse(responseCode = "400", description = "Cannot delete - referential integrity constraint violation")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'delete')")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteAppRoles(@RequestBody List<Long> ids) throws DataIntegrityException {
        appRoleFacade.bulkDeleteAppRoles(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create or update multiple app roles", responses = {
            @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed app roles",
                    content = @Content(schema = @Schema(implementation = AppRoleListRestEntity.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppRole', 'write')")
    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<AppRoleListRestEntity> createOrUpdateAllAppRoles(
            @RequestBody List<AppRoleRestEntity> restEntities
    ) {
        AppRoleListRestEntity result = appRoleFacade.createOrUpdateAllAppRoles(restEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}


