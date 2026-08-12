import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Unit, UnitList } from '../../model/unit/unit.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UnitsService {
  private http = inject(HttpClient);

  getUnits(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string) {
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
    
    if (query) {
      params = params.set('q', query);
    }
    
    const url = `${environment.apiBaseUrl}admin/api/units`;
    return this.http.get<UnitList>(url, { params });
  }

  getUnit(symbol: string): Observable<Unit> {
    const url = `${environment.apiBaseUrl}admin/api/units/${encodeURIComponent(symbol)}`;
    const params = new HttpParams()
      .set('$expand', '$includes,$info,$meta');
    return this.http.get<Unit>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/units/$meta`);
  }

  createUnit(unit: Unit): Observable<Unit> {
    const url = `${environment.apiBaseUrl}admin/api/units/create`;
    return this.http.post<Unit>(url, unit);
  }

  updateUnit(symbol: string, unit: Unit): Observable<Unit> {
    const url = `${environment.apiBaseUrl}admin/api/units/${encodeURIComponent(symbol)}`;
    return this.http.put<Unit>(url, unit);
  }

  patchUnit(symbol: string, patch: JsonPatchOperation[]): Observable<Unit> {
    const url = `${environment.apiBaseUrl}admin/api/units/${encodeURIComponent(symbol)}`;
    return this.http.patch<Unit>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteUnit(symbol: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/units/${encodeURIComponent(symbol)}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteUnits(symbols: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/units/bulk-delete`;
    return this.http.post<void>(url, symbols);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
