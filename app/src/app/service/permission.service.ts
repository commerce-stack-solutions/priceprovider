import { Injectable, inject, signal, effect, computed } from '@angular/core';
import { OAuthService, OAuthEvent } from 'angular-oauth2-oidc';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { AppRolesService } from './approle/app-role.service';

/**
 * PermissionService maps the current user's roles (extracted from the JWT) to
 * their permissions by loading each role's permissionRefs from the backend.
 *
 * Permissions follow the pattern: priceprovider.admin:<DataType>:<read|write|delete>
 * e.g. priceprovider.admin:Channel:read
 */
@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private authService = inject(AuthService);
  private appRolesService = inject(AppRolesService);
  private oauthService = inject(OAuthService);

  /** All permission strings the current user holds (empty when not logged in). */
  userPermissions = signal<Set<string>>(new Set());

  /** Reactive signal: true while permissions are being loaded from the backend. */
  loading = signal(false);

  /** Computed signal: true if the user has any 'priceprovider.admin' permission. */
  hasAdminPermission = computed(() => {
    const permissions = this.userPermissions();
    for (const p of permissions) {
      if (p.startsWith('priceprovider.admin:')) {
        return true;
      }
    }
    return false;
  });

  constructor() {
    // React to OAuth token events
    this.oauthService.events.subscribe((event: OAuthEvent) => {
      if (event.type === 'token_received' || event.type === 'token_refreshed') {
        this.loadPermissions();
      } else if (event.type === 'logout') {
        this.userPermissions.set(new Set());
      }
    });

    // Load permissions immediately if already authenticated (e.g. page refresh with valid session)
    if (this.authService.isAuthenticated()) {
      this.loadPermissions();
    }
  }

  /**
   * Fetches permission refs for every role the current user has and accumulates
   * them into the `userPermissions` signal.
   */
  loadPermissions(): void {
    const roles = this.authService.userRoles();
    if (!roles.length) {
      this.userPermissions.set(new Set());
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    const roleRequests = roles.map(roleName =>
      this.appRolesService.getAppRoleByName(roleName).pipe(
        map(role => role.permissionRefs ?? []),
        catchError(() => of([] as string[]))
      )
    );

    forkJoin(roleRequests).subscribe({
      next: (permissionArrays) => {
        const allPermissions = new Set<string>();
        permissionArrays.forEach(perms => perms.forEach(p => allPermissions.add(this.normalizePermission(p))));
        this.userPermissions.set(allPermissions);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  /** Returns true when the user holds the exact permission string. */
  hasPermission(permission: string): boolean {
    return this.userPermissions().has(this.normalizePermission(permission));
  }

  /** Returns true when the user can read entities of the given data type. */
  hasReadPermission(dataType: string): boolean {
    return this.hasPermissionForAction(dataType, 'read');
  }

  /** Returns true when the user can create/update entities of the given data type. */
  hasWritePermission(dataType: string): boolean {
    return this.hasPermissionForAction(dataType, 'write');
  }

  /** Returns true when the user can delete entities of the given data type. */
  hasDeletePermission(dataType: string): boolean {
    return this.hasPermissionForAction(dataType, 'delete');
  }

  /**
   * Returns true if the user has ANY permission for the given dataType and action,
   * including permissions with selectors.
   *
   * Examples:
   * - priceprovider.admin:PriceRow:read (global permission)
   * - priceprovider.admin:PriceRow[currencyRef=='EUR']:read (selector-based permission)
   *
   * Both would return true for hasPermissionForAction('PriceRow', 'read')
   */
  private hasPermissionForAction(dataType: string, action: string): boolean {
    const permissions = this.userPermissions();
    const prefix = `priceprovider.admin:${dataType}`;
    const suffix = `:${action}`;

    for (const permission of permissions) {
      if (permission.startsWith(prefix) && permission.endsWith(suffix)) {
        // Check if it's either a direct match or has a selector in between
        const middle = permission.substring(prefix.length, permission.length - suffix.length);
        if (middle === '' || middle.startsWith('[')) {
          return true;
        }
      }
    }

    return false;
  }

  /** Delegates to AuthService. */
  isLoggedIn(): boolean {
    return this.authService.isAuthenticated();
  }

  private normalizePermission(permission: string): string {
    return permission.replace(/\//g, ':');
  }
}
