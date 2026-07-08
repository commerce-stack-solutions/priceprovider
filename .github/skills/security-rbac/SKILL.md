---
name: security-rbac
description: 'Skill for implementing RBAC (Role-Based Access Control) and OIDC security in both backend service and frontend app'
---

# Goal
Implement or extend RBAC security features including permission-based access control, JWT authentication, role management, and organization-scoped data access in both the backend service and frontend application.

# Overview
The Price Provider system uses a fine-grained RBAC model where:
- **AppPermissions** define granular access rights (e.g., `priceprovider.admin:Channel:read`)
- **AppRoles** group permissions into logical roles (e.g., Admin, Contributor, Reader)
- **Users** are assigned roles in the Identity Provider (Keycloak)
- **Organization context** scopes data access based on hierarchical groups

# RBAC Model Structure

## Permissions
Permissions follow the pattern: `<scope>:<DataType>:<Action>`

**Examples:**
- `priceprovider.admin:Channel:read` - Read channels in admin API
- `priceprovider.admin:PriceRow:write` - Create/update price rows
- `priceprovider.admin:Unit:delete` - Delete units
- `priceprovider.public:PriceRow:read` - Read prices via public API

**Actions:**
- `read` - GET endpoints
- `write` - PUT, PATCH, POST endpoints
- `delete` - DELETE endpoints

## Roles
Roles are collections of permissions defined in `AppRole.0010.json`:

- **priceprovider.admin:Admin** - Full admin access to all data types
- **priceprovider.admin:Contributor** - Read and write access to all data types
- **priceprovider.admin:Reader** - Read-only access to all data types
- **priceprovider.admin:ChannelContributor** - Full access to channels only
- **priceprovider.public:PriceRowReader** - Public price API access (organization-scoped)

# Backend Implementation

## Step 1: Define Permissions and Roles

**Location:** `service/src/main/resources/initialize/essential/`

**AppPermission.0010.json:**
```json
[
  {
    "id": "priceprovider.admin:YourEntity:read",
    "name": "Read YourEntity",
    "description": "Permission to read YourEntity data"
  },
  {
    "id": "priceprovider.admin:YourEntity:write",
    "name": "Write YourEntity",
    "description": "Permission to create and update YourEntity data"
  },
  {
    "id": "priceprovider.admin:YourEntity:delete",
    "name": "Delete YourEntity",
    "description": "Permission to delete YourEntity data"
  }
]
```

**AppRole.0010.json:**
```json
[
  {
    "id": "priceprovider.admin:Admin",
    "name": "Administrator",
    "description": "Full system administrator",
    "permissionRefs": [
      "priceprovider.admin:YourEntity:read",
      "priceprovider.admin:YourEntity:write",
      "priceprovider.admin:YourEntity:delete",
      "... other permissions ..."
    ]
  }
]
```

## Step 2: Protect Controller Endpoints

Use `@PreAuthorize` annotations on controller methods:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/admin/api/yourentities")
public class YourEntityController {

    @GetMapping
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:read')")
    public YourListRestEntity getAll(...) {
        // Implementation
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:read')")
    public YourRestEntity getById(@PathVariable String id) {
        // Implementation
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:write')")
    public YourRestEntity create(@RequestBody @Valid YourRestEntity entity) {
        // Implementation
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:write')")
    public YourRestEntity update(@PathVariable String id, @RequestBody @Valid YourRestEntity entity) {
        // Implementation
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:write')")
    public YourRestEntity patch(@PathVariable String id, @RequestBody JsonPatch patch) {
        // Implementation
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:delete')")
    public void delete(@PathVariable String id) {
        // Implementation
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAuthority('priceprovider.admin:YourEntity:delete')")
    public void bulkDelete(@RequestBody List<String> ids) {
        // Implementation
    }
}
```

## Step 3: JWT Configuration

The system automatically maps JWT claims to Spring Security authorities. This is configured in `SecurityConfig`:

**Key components:**
- `JwtClaimsExtractor` - Extracts roles and organization from JWT
- `JwtAuthenticationConverter` - Converts claims to `GrantedAuthority` objects
- Role extraction order:
  1. Client-specific roles: `resource_access.<clientId>.roles`
  2. Realm roles: `realm_access.roles` (fallback)

**No additional code needed** - this is already configured in the system.

## Step 4: Organization Context Extraction

For organization-scoped access (Public API), the system extracts organization from `groups` claim:

**Logic:**
- Looks for groups starting with `/organizations/`
- Finds the "deepest" path (most `/` characters)
- Returns the last segment as organization ID

**Example:**
- User in: `/organizations/ORG-CITY-COUNCIL/ORG-CITY-HEALTH`
- Extracted organization: `ORG-CITY-COUNCIL/ORG-CITY-HEALTH`

This is handled automatically by `JwtClaimsExtractor` - no additional code needed.

# Frontend Implementation

## Step 1: Setup OIDC Configuration

**Location:** `app/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  oidc: {
    issuerUri: 'http://localhost:8080/realms/priceprovider',
    clientId: 'priceprovider-app',
    scope: 'openid profile email offline_access',
    requireHttps: false,
  },
  apiUrl: 'http://localhost:8080'
};
```

## Step 2: Use Permission Service

**Inject PermissionService:**
```typescript
import { Component, computed, inject } from '@angular/core';
import { PermissionService } from '../service/permission.service';
import { SessionService } from '../service/session.service';

@Component({
  selector: 'app-your-component',
  // ...
})
export class YourComponent {
  protected permissionService = inject(PermissionService);
  private sessionService = inject(SessionService);
  lang = computed(() => this.sessionService.language());
}
```

**Check permissions in templates:**
```html
<!-- Show/hide based on read permission -->
@if (permissionService.hasReadPermission('YourEntity')) {
  <a [routerLink]="['/' + lang(), 'yourentities']">Your Entities</a>
}

<!-- Show/hide based on write permission -->
@if (permissionService.hasWritePermission('YourEntity')) {
  <button (click)="onEdit()">Edit</button>
}

<!-- Show/hide based on delete permission -->
@if (permissionService.hasDeletePermission('YourEntity')) {
  <button (click)="onDelete()">Delete</button>
}

<!-- Check specific permission string -->
@if (permissionService.hasPermission('priceprovider.admin:YourEntity:write')) {
  <button>Custom Action</button>
}
```

**Check permissions in code:**
```typescript
// In ngOnInit, redirect if user lacks write permission (do not use alert())
ngOnInit(): void {
  if (!this.permissionService.hasWritePermission('YourEntity')) {
    this.router.navigate(['/' + this.lang(), 'yourentities']);
    return;
  }
  // Proceed with loading edit form
}
```

## Step 3: Load Permissions on Login

Permissions are automatically loaded by `PermissionService.loadPermissions()` after authentication. This method:

1. Gets user roles from JWT token
2. Fetches each role's permissions from `/admin/api/app-roles/{roleId}`
3. Caches permissions in memory
4. Normalizes permission IDs to colon format

**Manual permission reload** (e.g., after loading new data):
```typescript
import { PermissionService } from '../service/permission.service';

export class ServiceInitializationComponent {
  permissionService = inject(PermissionService);

  async onLoadData() {
    await this.dataService.loadData();
    // Reload permissions after loading new roles/permissions
    await this.permissionService.loadPermissions();
  }
}
```

## Step 4: Add to Navigation Menu

**sidebar.component.html:**
```html
@if (permissionService.hasReadPermission('YourEntity')) {
  <a [routerLink]="['/' + lang(), 'yourentities']" class="list-group-item list-group-item-action py-2 ripple" routerLinkActive="active">
    <i class="bi bi-your-icon me-3"></i><span>{{ 'components.sidebar.yourEntities' | transloco }}</span>
  </a>
}
```

**home.component.html:**
```html
@if (permissionService.hasReadPermission('YourEntity')) {
  <div class="col-md-4">
    <div class="card">
      <div class="card-body">
        <h5 class="card-title">{{ 'pages.yourEntities.title' | transloco }}</h5>
        <p class="card-text">{{ 'pages.yourEntities.description' | transloco }}</p>
        <a [routerLink]="['/' + lang(), 'yourentities']" class="btn btn-primary">{{ 'pages.yourEntities.viewAll' | transloco }}</a>
      </div>
    </div>
  </div>
}
```

# Testing Security

## Backend Tests

**Test unauthorized access:**
```java
@Test
@WithMockUser(authorities = {})  // No permissions
public void testGetYourEntities_Unauthorized() throws Exception {
    mockMvc.perform(get("/admin/api/yourentities"))
        .andExpect(status().isForbidden());
}
```

**Test authorized access:**
```java
@Test
@WithMockUser(authorities = {"priceprovider.admin:YourEntity:read"})
public void testGetYourEntities_Authorized() throws Exception {
    mockMvc.perform(get("/admin/api/yourentities"))
        .andExpect(status().isOk());
}
```

## Frontend Tests

**Test permission-based rendering:**
```typescript
it('should show edit button when user has write permission', () => {
  spyOn(permissionService, 'hasWritePermission').and.returnValue(true);
  fixture.detectChanges();
  const editButton = fixture.nativeElement.querySelector('.edit-button');
  expect(editButton).toBeTruthy();
});

it('should hide edit button when user lacks write permission', () => {
  spyOn(permissionService, 'hasWritePermission').and.returnValue(false);
  fixture.detectChanges();
  const editButton = fixture.nativeElement.querySelector('.edit-button');
  expect(editButton).toBeFalsy();
});
```

# Identity Provider Setup (Keycloak)

## Create Client Role in Keycloak

1. Navigate to Clients → `priceprovider-service` → Roles
2. Create new role matching the AppRole ID (e.g., `priceprovider.admin:Admin`)
3. Assign role to users as needed

## Create Organization Groups

1. Navigate to Groups
2. Create group hierarchy under `/organizations/`
   - Example: `/organizations/ORG-MYCOMPANY`
   - Example: `/organizations/ORG-MYCOMPANY/ORG-DEPARTMENT-A`
3. Assign users to appropriate groups

# Best Practices

## Backend
1. **Always use @PreAuthorize** on controller methods to enforce access control
2. **Follow naming convention** for permissions: `<scope>:<DataType>:<Action>`
3. **Group related permissions** into logical roles
4. **Update AppRole.0010.json** when adding new permissions to existing roles
5. **Test both authorized and unauthorized access** in integration tests

## Frontend
1. **Use PermissionService helpers** (`hasReadPermission`, `hasWritePermission`, `hasDeletePermission`) instead of checking permission strings directly
2. **Show/hide UI elements** based on permissions (don't just disable them)
3. **Check permissions before actions** (both in template and in component code)
4. **Reload permissions** after operations that might change roles/permissions
5. **Handle missing permissions gracefully** with user-friendly error messages

## Security Considerations
1. **Backend is source of truth** - Frontend permission checks are for UX only; backend must enforce access control
2. **Never rely on frontend security** alone - Always validate on the server
3. **Log access denials** for security auditing
4. **Use HTTPS in production** - Set `requireHttps: true` in OIDC config
5. **Rotate secrets regularly** - Client secrets, signing keys, etc.

# Common Patterns

## Pattern 1: Entity CRUD Permissions
For each entity, create three permissions:
- `<scope>:<Entity>:read` - GET endpoints
- `<scope>:<Entity>:write` - POST, PUT, PATCH endpoints
- `<scope>:<Entity>:delete` - DELETE endpoints

## Pattern 2: Custom Action Permissions
For special operations, create dedicated permissions:
- `priceprovider.admin:ServiceInitialization:write` - Load sample data
- `priceprovider.admin:PriceRow:bulk-delete` - Bulk delete operations

## Pattern 3: Organization-Scoped Access
For public APIs, use organization context:
- Extract from JWT `groups` claim
- Filter results by organization
- Return only data matching user's organization

# Troubleshooting

## "403 Forbidden" errors
- Check JWT token contains expected roles in `resource_access.<clientId>.roles` or `realm_access.roles`
- Verify AppRole exists in database with correct permission references
- Check `@PreAuthorize` annotation uses exact permission string
- Ensure user is assigned the role in Keycloak

## Permissions not loading in frontend
- Check browser console for errors from `/admin/api/app-roles/{id}` calls
- Verify user has `priceprovider.admin:AppRole:read` permission
- Ensure AppRole entities exist in database
- Check `PermissionService.loadPermissions()` is called after login

## Organization not extracted correctly
- Verify JWT token contains `groups` claim
- Check group path starts with `/organizations/` (configurable prefix)
- Ensure user is member of organization group in Keycloak

# Relevant Resources

- [Security Implementation Guide (Service)](../../../service/doc/020-development/021-security-implementation-guide.md) - Backend security technical details
- [Security Implementation Guide (App)](../../../app/doc/020-development/020-security-implementation-guide.md) - Frontend security patterns
- [RBAC and User Guide](../../../service/doc/030-features/050-rbac-and-user-guide.md) - RBAC model overview and sample users
- Sample Data:
  - `service/src/main/resources/initialize/essential/AppPermission.0010.json`
  - `service/src/main/resources/initialize/essential/AppRole.0010.json`
