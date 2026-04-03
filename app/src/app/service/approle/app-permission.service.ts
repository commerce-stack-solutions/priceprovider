import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AppPermission, AppPermissionList } from '../../model/approle/app-permission.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AppPermissionsService {
  private http = inject(HttpClient);

  getAppPermissions(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, expand?: string, query?: string): Observable<AppPermissionList> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('page-size', pageSize.toString());

    if (sortBy && sortBy.length > 0) {
      sortBy.forEach(field => {
        params = params.append('sort-by', field);
      });
      if (sortDirection) {
        params = params.set('sort-direction', sortDirection);
      }
    }

    if (expand) {
      params = params.set('$expand', expand);
    }

    if (query) {
      params = params.set('q', query);
    }

    const url = `${environment.apiBaseUrl}admin/api/app-permissions`;
    return this.http.get<AppPermissionList>(url, { params });
  }

  getAppPermission(name: string, expand?: string): Observable<AppPermission> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/${encodeURIComponent(name).replace(/%3A/gi, ":")}`;
    let params = new HttpParams();
    if (expand) {
      params = params.set('$expand', expand);
    } else {
      params = params.set('$expand', '$includes,$info,$meta');
    }
    return this.http.get<AppPermission>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/$meta`;
    return this.http.get<MetaInfo>(url);
  }

  createAppPermission(permission: AppPermission): Observable<AppPermission> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/create`;
    return this.http.post<AppPermission>(url, permission);
  }

  updateAppPermission(name: string, permission: AppPermission): Observable<AppPermission> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/${encodeURIComponent(name).replace(/%3A/gi, ":")}`;
    return this.http.put<AppPermission>(url, permission);
  }

  patchAppPermission(name: string, patch: JsonPatchOperation[]): Observable<AppPermission> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/${encodeURIComponent(name).replace(/%3A/gi, ":")}`;
    return this.http.patch<AppPermission>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteAppPermission(name: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/${encodeURIComponent(name).replace(/%3A/gi, ":")}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteAppPermissions(ids: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/bulk-delete`;
    return this.http.post<void>(url, ids);
  }

  bulkCreateOrUpdateAppPermissions(permissions: AppPermission[]): Observable<AppPermission[]> {
    const url = `${environment.apiBaseUrl}admin/api/app-permissions/bulk-create-or-update`;
    return this.http.post<AppPermission[]>(url, permissions);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
