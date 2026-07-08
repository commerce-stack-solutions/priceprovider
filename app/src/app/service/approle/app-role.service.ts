import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AppRole, AppRoleList } from '../../model/approle/app-role.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AppRolesService {
  private http = inject(HttpClient);

  getAppRoles(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, expand?: string, query?: string): Observable<AppRoleList> {
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

    const url = `${environment.apiBaseUrl}admin/api/app-roles`;
    return this.http.get<AppRoleList>(url, { params });
  }

  getAppRole(id: number, expand?: string): Observable<AppRole> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/${id}`;
    let params = new HttpParams();
    if (expand) {
      params = params.set('$expand', expand);
    } else {
      params = params.set('$expand', '$includes,$info,$meta');
    }
    return this.http.get<AppRole>(url, { params });
  }

  getAppRoleByName(name: string, expand?: string): Observable<AppRole> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/by-name/${encodeURIComponent(name)}`;
    let params = new HttpParams();
    if (expand) {
      params = params.set('$expand', expand);
    } else {
      params = params.set('$expand', '$includes,$info');
    }
    return this.http.get<AppRole>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/$meta`;
    return this.http.get<MetaInfo>(url);
  }

  createAppRole(role: Partial<AppRole>): Observable<AppRole> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/create`;
    return this.http.post<AppRole>(url, role);
  }

  updateAppRole(id: number, role: Partial<AppRole>): Observable<AppRole> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/${id}`;
    return this.http.put<AppRole>(url, role);
  }

  patchAppRole(id: number, patch: JsonPatchOperation[]): Observable<AppRole> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/${id}`;
    return this.http.patch<AppRole>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteAppRole(id: number): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/${id}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteAppRoles(ids: number[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/bulk-delete`;
    return this.http.post<void>(url, ids);
  }

  bulkCreateOrUpdateAppRoles(roles: Partial<AppRole>[]): Observable<AppRole[]> {
    const url = `${environment.apiBaseUrl}admin/api/app-roles/bulk-create-or-update`;
    return this.http.post<AppRole[]>(url, roles);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
