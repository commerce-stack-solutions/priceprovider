import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../service/auth.service';
import { environment } from '../environments/environment';

/**
 * HTTP interceptor that attaches the JWT Bearer token to outgoing requests
 * targeting the Price Provider Service API.
 *
 * Only attaches the token to requests targeting the configured API base URL
 * to avoid leaking credentials to third-party services.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Only attach token to requests targeting our API
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const token = authService.getAccessToken();
  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};
