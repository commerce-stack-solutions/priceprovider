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
import io.commercestacksolutions.priceproviderservice.facade.approle.AppPermissionFacade;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppPermissionListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/api/app-permissions")
@Tag(name = "AppPermissions", description = "Application permission management API")
public class AppPermissionController {

    private final AppPermissionFacade appPermissionFacade;

    @Autowired
    public AppPermissionController(AppPermissionFacade appPermissionFacade) {
        this.appPermissionFacade = appPermissionFacade;
    }

    @Operation(summary = "Get list of app permissions", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved app permissions",
                    content = @Content(schema = @Schema(implementation = AppPermissionListRestEntity.class)))
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'read')")
    @GetMapping
    public AppPermissionListRestEntity getAppPermissions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,
            @RequestParam(value = "sort-direction", required = false) String sortDirection,
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return appPermissionFacade.getAppPermissions(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(summary = "Get app permission by id", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved app permission"),
            @ApiResponse(responseCode = "404", description = "App permission not found")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'read')")
    @GetMapping("/{id}")
    public AppPermissionRestEntity getAppPermission(
            @PathVariable("id") Long id,
            @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        return appPermissionFacade.getAppPermission(id, expand);
    }

    @Operation(summary = "Get meta information for app permissions")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return appPermissionFacade.getMeta();
    }

    @Operation(summary = "Partially update app permission", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully patched app permission"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "App permission not found")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'write')")
    @PatchMapping("/{id}")
    public AppPermissionRestEntity patch(
            @PathVariable("id") Long id,
            @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return appPermissionFacade.patch(id, patch);
    }

    @Operation(summary = "Create or update app permission", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully created or updated app permission"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'write')")
    @PutMapping("/{id}")
    public AppPermissionRestEntity createOrRecreate(
            @PathVariable("id") Long id,
            @RequestBody AppPermissionRestEntity restEntity
    ) throws DataMappingException, EntityValidationException, NotFoundException {
        return appPermissionFacade.createOrRecreate(id, restEntity);
    }

    @Operation(summary = "Create new app permission", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully created app permission"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "App permission with the same ID already exists")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'write')")
    @PostMapping("/create")
    public AppPermissionRestEntity create(
            @RequestBody AppPermissionRestEntity restEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return appPermissionFacade.create(restEntity);
    }

    @Operation(summary = "Delete app permission", responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted app permission"),
            @ApiResponse(responseCode = "404", description = "App permission not found")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            appPermissionFacade.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Bulk delete app permissions", responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted app permissions"),
            @ApiResponse(responseCode = "400", description = "Cannot delete - referential integrity constraint violation")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'delete')")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteAppPermissions(@RequestBody List<Long> ids) throws DataIntegrityException {
        appPermissionFacade.bulkDeleteAppPermissions(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create or update multiple app permissions", responses = {
            @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed app permissions",
                    content = @Content(schema = @Schema(implementation = AppPermissionListRestEntity.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size")
    })
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('AppPermission', 'write')")
    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<AppPermissionListRestEntity> createOrUpdateAllAppPermissions(
            @RequestBody List<AppPermissionRestEntity> restEntities
    ) {
        AppPermissionListRestEntity result = appPermissionFacade.createOrUpdateAllAppPermissions(restEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}


