import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Group, GroupList } from '../../model/group/group.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GroupsService {
  private http = inject(HttpClient);

  getGroups(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, expand?: string, query?: string): Observable<GroupList> {
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
    
    const url = `${environment.apiBaseUrl}admin/api/groups`;
    return this.http.get<GroupList>(url, { params });
  }

  getGroup(id: string, expand?: string): Observable<Group> {
    const url = `${environment.apiBaseUrl}admin/api/groups/${encodeURIComponent(id)}`;
    let params = new HttpParams();
    if (expand) {
      params = params.set('$expand', expand);
    } else {
      params = params.set('$expand', '$includes,$info,$meta');
    }
    return this.http.get<Group>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    const url = `${environment.apiBaseUrl}admin/api/groups/$meta`;
    return this.http.get<MetaInfo>(url);
  }

  createGroup(group: Group): Observable<Group> {
    const url = `${environment.apiBaseUrl}admin/api/groups/create`;
    return this.http.post<Group>(url, group);
  }

  updateGroup(id: string, group: Group): Observable<Group> {
    const url = `${environment.apiBaseUrl}admin/api/groups/${encodeURIComponent(id)}`;
    return this.http.put<Group>(url, group);
  }

  patchGroup(id: string, patch: JsonPatchOperation[]): Observable<Group> {
    const url = `${environment.apiBaseUrl}admin/api/groups/${encodeURIComponent(id)}`;
    return this.http.patch<Group>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteGroup(id: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/groups/${encodeURIComponent(id)}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteGroups(ids: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/groups/bulk-delete`;
    return this.http.post<void>(url, ids);
  }

  bulkCreateOrUpdateGroups(groups: Group[]): Observable<Group[]> {
    const url = `${environment.apiBaseUrl}admin/api/groups/bulk-create-or-update`;
    return this.http.post<Group[]>(url, groups);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
