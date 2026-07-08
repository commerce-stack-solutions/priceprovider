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
import io.commercestacksolutions.priceproviderservice.facade.group.GroupFacade;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/admin/api/groups")
@Tag(name = "Groups", description = "Group management API - handles organizational groups and promotional groups")
public class GroupController {

    private final GroupFacade groupFacade;

    @Autowired
    public GroupController(GroupFacade groupFacade) {
        this.groupFacade = groupFacade;
    }

    @Operation(
            summary = "Get list of groups",
            description = "Retrieves a paginated and sortable list with optional filtering of groups. Supports sorting by: id, name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved groups",
                            content = @Content(schema = @Schema(implementation = GroupListRestEntity.class)))
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'read')") 

    @GetMapping
    public GroupListRestEntity getGroups(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: id, name. Can be specified multiple times for multi-field sorting",
                    example = "name")
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,

            @Parameter(description = "Sort direction: asc (ascending) or desc (descending)", example = "asc",
                    schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(value = "sort-direction", required = false) String sortDirection,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        // QueryFilterRuntimeException is handled by ExceptionHandlerAdvice global handler
        return groupFacade.getGroups(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get group by id",
            description = "Retrieves a single group by its unique identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved group",
                            content = @Content(schema = @Schema(implementation = GroupRestEntity.class))),
                    @ApiResponse(responseCode = "404", description = "Group not found")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'read')") 

    @GetMapping("/{id}")
    public GroupRestEntity getGroup(
            @Parameter(description = "Group ID", example = "GRP-001")
            @PathVariable("id") String id,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws NotFoundException, DataMappingException {
        return groupFacade.getGroup(id, expand);
    }

    @Operation(
            summary = "Get meta information for groups",
            description = "Returns identity fields, mandatory fields and enum values for the Group entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return groupFacade.getMeta();
    }

    @Operation(
            summary = "Partially update group",
            description = "Applies JSON Patch operations to partially update a group. Supports operations: add, remove, replace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched group",
                            content = @Content(schema = @Schema(implementation = GroupRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Group not found")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'write')") 

    @PatchMapping("/{id}")
    public GroupRestEntity patch(
            @Parameter(description = "Group ID", example = "GRP-001")
            @PathVariable("id") String id,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return groupFacade.patch(id, patch);
    }

    @Operation(
            summary = "Create or update group",
            description = "Creates a new group or fully replaces an existing group with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated group",
                            content = @Content(schema = @Schema(implementation = GroupRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'write')") 

    @PutMapping("/{id}")
    public GroupRestEntity createOrRecreate(
            @Parameter(description = "Group ID", example = "GRP-001")
            @PathVariable("id") String id,

            @Parameter(description = "Group data")
            @RequestBody GroupRestEntity groupRestEntity
    ) throws DataMappingException, EntityValidationException {
        return groupFacade.createOrRecreate(id, groupRestEntity);
    }

    @Operation(
            summary = "Create new group",
            description = "Creates a new group with a provided ID. The ID must be included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created group",
                            content = @Content(schema = @Schema(implementation = GroupRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Group with the same ID already exists")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'write')") 

    @PostMapping("/create")
    public GroupRestEntity create(
            @Parameter(description = "Group data including ID")
            @RequestBody GroupRestEntity groupRestEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return groupFacade.create(groupRestEntity);
    }

    @Operation(
            summary = "Delete group",
            description = "Deletes a group by its ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted group"),
                    @ApiResponse(responseCode = "404", description = "Group not found"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete group - it may be referenced by other entities")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'delete')") 

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Group ID", example = "GRP-001")
            @PathVariable("id") String id
    ) {
        try {
            groupFacade.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Bulk delete groups",
            description = "Deletes multiple groups by their IDs. Maximum 100 groups per request.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted groups"),
                    @ApiResponse(responseCode = "400", description = "Cannot delete - referential integrity constraint violation")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteGroups(
            @Parameter(description = "List of group IDs to delete")
            @RequestBody List<String> ids
    ) throws DataIntegrityException {
        groupFacade.bulkDeleteGroups(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple groups",
            description = "Creates new groups or updates existing ones in a single batch operation. Maximum 100 groups per request. " +
                    "Entities with existing IDs will be updated. Entities with new IDs will be created.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed groups",
                            content = @Content(schema = @Schema(implementation = GroupListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size",
                            content = @Content(schema = @Schema(implementation = GroupListRestEntity.class)))
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Group', 'write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<GroupListRestEntity> createOrUpdateAllGroups(
            @Parameter(description = "List of group data (max 100 items). ID is required for all entities.")
            @RequestBody List<GroupRestEntity> groupRestEntities
    ) {
        GroupListRestEntity result = groupFacade.createOrUpdateAllGroups(groupRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}



