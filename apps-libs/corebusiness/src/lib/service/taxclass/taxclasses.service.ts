import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { TaxClass, TaxClassList } from '../../model/taxclass/taxclass.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TaxClassesService {
  private http = inject(HttpClient);

  getTaxClasses(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string): Observable<TaxClassList> {
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
    
    const url = `${environment.apiBaseUrl}admin/api/taxclasses`;
    return this.http.get<TaxClassList>(url, { params });
  }

  getTaxClass(taxClassId: string): Observable<TaxClass> {
    const url = `${environment.apiBaseUrl}admin/api/taxclasses/${encodeURIComponent(taxClassId)}`;
    const params = new HttpParams()
      .set('$expand', '$includes,$info,$meta');
    return this.http.get<TaxClass>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/taxclasses/$meta`);
  }

  createTaxClass(taxClass: TaxClass): Observable<TaxClass> {
    const url = `${environment.apiBaseUrl}admin/api/taxclasses/create`;
    return this.http.post<TaxClass>(url, taxClass);
  }

  updateTaxClass(taxClassId: string, taxClass: TaxClass): Observable<TaxClass> {
    const url = `${environment.apiBaseUrl}admin/api/taxclasses/${encodeURIComponent(taxClassId)}`;
    return this.http.put<TaxClass>(url, taxClass);
  }

  patchTaxClass(taxClassId: string, patch: JsonPatchOperation[]): Observable<TaxClass> {
    const url = `${environment.apiBaseUrl}admin/api/taxclasses/${encodeURIComponent(taxClassId)}`;
    return this.http.patch<TaxClass>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteTaxClass(taxClassId: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/taxclasses/${encodeURIComponent(taxClassId)}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteTaxClasses(taxClassIds: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/taxclasses/bulk-delete`;
    return this.http.post<void>(url, taxClassIds);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
