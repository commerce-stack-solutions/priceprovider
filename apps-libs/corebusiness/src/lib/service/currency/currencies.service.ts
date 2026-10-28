import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Currency, CurrencyList } from '../../model/currency/currency.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CurrenciesService {
  private http = inject(HttpClient);

  getCurrencies(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string): Observable<CurrencyList> {
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
    
    const url = `${environment.apiBaseUrl}admin/api/currencies`;
    return this.http.get<CurrencyList>(url, { params });
  }

  getCurrency(currencyKey: string): Observable<Currency> {
    const url = `${environment.apiBaseUrl}admin/api/currencies/${encodeURIComponent(currencyKey)}`;
    const params = new HttpParams()
      .set('$expand', '$includes,$info,$meta');
    return this.http.get<Currency>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/currencies/$meta`);
  }

  createCurrency(currency: Currency): Observable<Currency> {
    const url = `${environment.apiBaseUrl}admin/api/currencies/create`;
    return this.http.post<Currency>(url, currency);
  }

  updateCurrency(currencyKey: string, currency: Currency): Observable<Currency> {
    const url = `${environment.apiBaseUrl}admin/api/currencies/${encodeURIComponent(currencyKey)}`;
    return this.http.put<Currency>(url, currency);
  }

  patchCurrency(currencyKey: string, patch: JsonPatchOperation[]): Observable<Currency> {
    const url = `${environment.apiBaseUrl}admin/api/currencies/${encodeURIComponent(currencyKey)}`;
    return this.http.patch<Currency>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteCurrency(currencyKey: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/currencies/${encodeURIComponent(currencyKey)}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteCurrencies(currencyKeys: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/currencies/bulk-delete`;
    return this.http.post<void>(url, currencyKeys);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
