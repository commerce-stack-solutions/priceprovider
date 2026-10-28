import { Injectable, signal } from '@angular/core';
import { OAuthService, AuthConfig, OAuthEvent } from 'angular-oauth2-oidc';
import { environment } from '../environments/environment';

export interface UserProfile {
  sub: string;
  preferred_username?: string;
  name?: string;
  email?: string;
  groups?: string[];
}

/**
 * AuthService wraps the angular-oauth2-oidc OAuthService to provide OIDC login/logout
 * and JWT token access for the Price Provider App.
 *
 * Configures Authorization Code Flow with PKCE using the Keycloak realm defined
 * in the application environment.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private static readonly ORGANIZATION_PATH_PREFIX = '/organizations/';

  /** Reactive signal: true when the user is authenticated */
  isAuthenticated = signal(false);

  /** Reactive signal: the current user profile (null when not authenticated) */
  userProfile = signal<UserProfile | null>(null);

  /** Reactive signal: role IDs extracted from the JWT access token */
  userRoles = signal<string[]>([]);

  constructor(private oauthService: OAuthService) {
    this.configure();
  }

  private configure(): void {
    const authConfig: AuthConfig = {
      issuer: environment.oidc.issuerUri,
      redirectUri: window.location.origin + '/',
      postLogoutRedirectUri: window.location.origin + '/',
      clientId: environment.oidc.clientId,
      scope: environment.oidc.scope,
      responseType: 'code',
      requireHttps: environment.oidc.requireHttps,
      showDebugInformation: false,
      clearHashAfterLogin: true,
    };

    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();

    this.oauthService.events.subscribe((event: OAuthEvent) => {
      if (event.type === 'token_received' || event.type === 'token_refreshed') {
        this.updateAuthState();
      } else if (event.type === 'logout') {
        this.isAuthenticated.set(false);
        this.userProfile.set(null);
        this.userRoles.set([]);
      }
    });
  }

  /**
   * Initializes OIDC: loads discovery document and handles redirect callback.
   * Must be called during application startup (e.g. in APP_INITIALIZER).
   */
  async initialize(): Promise<void> {
    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      this.updateAuthState();
    } catch {
      // OIDC provider unavailable (e.g. local dev without Keycloak) – continue without auth
    }
  }

  /** Initiates the OIDC login flow (redirects to Keycloak). */
  login(): void {
    this.oauthService.initCodeFlow();
  }

  /** Logs out the user and clears all tokens. */
  logout(): void {
    this.oauthService.logOut();
  }

  /** Returns the current Bearer token, or null if not authenticated. */
  getAccessToken(): string | null {
    const token = this.oauthService.getAccessToken();
    return token || null;
  }

  /**
   * Returns the effective organization filter derived from the deepest
   * organization group path in the token's 'groups' claim.
   * Returns null for anonymous or unauthenticated users.
   */
  getEffectiveOrganization(): string | null {
    const profile = this.userProfile();
    if (!profile?.groups?.length) return null;

    const prefix = AuthService.ORGANIZATION_PATH_PREFIX;
    const orgGroups = profile.groups
      .filter(g => g && g.startsWith(prefix))
      .sort((a, b) => b.split('/').length - a.split('/').length); // deepest first

    if (!orgGroups.length) return null;

    const deepestPath = orgGroups[0].substring(prefix.length);
    const parts = deepestPath.split('/');
    return parts[parts.length - 1]; // last segment = deepest department
  }

  private updateAuthState(): void {
    const hasValidToken = this.oauthService.hasValidAccessToken();
    this.isAuthenticated.set(hasValidToken);

    if (hasValidToken) {
      const claims = this.oauthService.getIdentityClaims() as UserProfile;
      this.userProfile.set(claims || null);
      this.userRoles.set(this.parseRolesFromAccessToken());
    } else {
      this.userProfile.set(null);
      this.userRoles.set([]);
    }
  }

  /**
   * Decodes the JWT access token and extracts role names.
   * Checks resource_access.<clientId>.roles first (client roles),
   * then falls back to realm_access.roles (realm-wide roles).
   */
  private parseRolesFromAccessToken(): string[] {
    const token = this.oauthService.getAccessToken();
    if (!token) return [];

    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const payload = JSON.parse(jsonPayload);

      // Try client-specific roles first
      const clientId = environment.oidc.clientId;
      const clientRoles: string[] | undefined = payload?.resource_access?.[clientId]?.roles;
      if (clientRoles?.length) return clientRoles;

      // Try alternate client ID used in some deployments
      const altClientRoles: string[] | undefined = payload?.resource_access?.['priceproviderservice']?.roles;
      if (altClientRoles?.length) return altClientRoles;

      // Fall back to realm-level roles
      return payload?.realm_access?.roles ?? [];
    } catch {
      return [];
    }
  }
}
