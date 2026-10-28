
import {
  ApplicationConfig,
  APP_INITIALIZER,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { translocoConfig } from './transloco-config';
import { AuthService } from 'core';
import { authInterceptor } from './shared/auth.interceptor';
import { MenuRegistryLoader } from './menu-registry.loader';

function initializeAuth(authService: AuthService): () => Promise<void> {
  return () => authService.initialize();
}

function initializeMenuRegistry(menuRegistryLoader: MenuRegistryLoader): () => Promise<void> {
  return () => menuRegistryLoader.load();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideOAuthClient(),
    translocoConfig,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeMenuRegistry,
      deps: [MenuRegistryLoader],
      multi: true
    }
  ]
};
