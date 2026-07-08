import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { PriceRow, PriceRowList } from '../../model/pricerow/price-row.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PricerowsService {
  private http = inject(HttpClient);

  getPriceRows(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string) {
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
    
    const url = `${environment.apiBaseUrl}admin/api/pricerows`;
    return this.http.get<PriceRowList>(url, { params });
  }

  getPriceRow(id: string): Observable<PriceRow> {
    const url = `${environment.apiBaseUrl}admin/api/pricerows/${id}`;
    // Request taxation info and all includes (unit, currency, taxClass)
    const params = new HttpParams()
      .set('$expand', '$info,$includes,$meta');
    return this.http.get<PriceRow>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/pricerows/$meta`);
  }

  createPriceRow(priceRow: PriceRow): Observable<PriceRow> {
    const url = `${environment.apiBaseUrl}admin/api/pricerows/create`;
    return this.http.post<PriceRow>(url, priceRow);
  }

  updatePriceRow(id: string, priceRow: PriceRow): Observable<PriceRow> {
    const url = `${environment.apiBaseUrl}admin/api/pricerows/${id}`;
    return this.http.put<PriceRow>(url, priceRow);
  }

  patchPriceRow(id: string, patch: JsonPatchOperation[]): Observable<PriceRow> {
    const url = `${environment.apiBaseUrl}admin/api/pricerows/${id}`;
    return this.http.patch<PriceRow>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deletePriceRow(id: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/pricerows/${id}`;
    return this.http.delete<void>(url);
  }

  bulkDeletePriceRows(ids: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/pricerows/bulk-delete`;
    return this.http.post<void>(url, ids);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
