import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Organization, OrganizationList } from '../../model/organization/organization.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class OrganizationsService {
  private http = inject(HttpClient);

  getOrganizations(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, expand?: string, query?: string): Observable<OrganizationList> {
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
    
    const url = `${environment.apiBaseUrl}admin/api/organizations`;
    return this.http.get<OrganizationList>(url, { params });
  }

  getOrganization(id: string, expand?: string): Observable<Organization> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/${encodeURIComponent(id)}`;
    let params = new HttpParams();
    if (expand) {
      params = params.set('$expand', expand);
    } else {
      params = params.set('$expand', '$includes,$info,$meta');
    }
    return this.http.get<Organization>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/$meta`;
    return this.http.get<MetaInfo>(url);
  }

  createOrganization(organization: Organization): Observable<Organization> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/create`;
    return this.http.post<Organization>(url, organization);
  }

  updateOrganization(id: string, organization: Organization): Observable<Organization> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/${encodeURIComponent(id)}`;
    return this.http.put<Organization>(url, organization);
  }

  patchOrganization(id: string, patch: JsonPatchOperation[]): Observable<Organization> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/${encodeURIComponent(id)}`;
    return this.http.patch<Organization>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteOrganization(id: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/${encodeURIComponent(id)}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteOrganizations(ids: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/bulk-delete`;
    return this.http.post<void>(url, ids);
  }

  bulkDelete(ids: string[]): Observable<void> {
    return this.bulkDeleteOrganizations(ids);
  }

  bulkCreateOrUpdateOrganizations(organizations: Organization[]): Observable<Organization[]> {
    const url = `${environment.apiBaseUrl}admin/api/organizations/bulk-create-or-update`;
    return this.http.post<Organization[]>(url, organizations);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
