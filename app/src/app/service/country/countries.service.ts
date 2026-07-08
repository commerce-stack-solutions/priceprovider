import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Country, CountryList } from '../../model/country/country.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CountriesService {
  private http = inject(HttpClient);

  getCountries(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string): Observable<CountryList> {
    let params = new HttpParams().set('page', page.toString()).set('page-size', pageSize.toString());
    if (sortBy && sortBy.length > 0) { sortBy.forEach(field => { params = params.append('sort-by', field); }); if (sortDirection) { params = params.set('sort-direction', sortDirection); } }
    if (query) { params = params.set('q', query); }
    return this.http.get<CountryList>(`${environment.apiBaseUrl}admin/api/countries`, { params });
  }
  getCountry(isoKey: string): Observable<Country> {
    return this.http.get<Country>(`${environment.apiBaseUrl}admin/api/countries/${encodeURIComponent(isoKey)}`, { params: new HttpParams().set('$expand', '$includes,$info,$meta') });
  }
  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/countries/$meta`);
  }
  createCountry(country: Country): Observable<Country> { return this.http.post<Country>(`${environment.apiBaseUrl}admin/api/countries/create`, country); }
  updateCountry(isoKey: string, country: Country): Observable<Country> { return this.http.put<Country>(`${environment.apiBaseUrl}admin/api/countries/${encodeURIComponent(isoKey)}`, country); }
  patchCountry(isoKey: string, patch: any[]): Observable<Country> { return this.http.patch<Country>(`${environment.apiBaseUrl}admin/api/countries/${encodeURIComponent(isoKey)}`, patch, { headers: { 'Content-Type': 'application/json-patch+json' } }); }
  deleteCountry(isoKey: string): Observable<void> { return this.http.delete<void>(`${environment.apiBaseUrl}admin/api/countries/${encodeURIComponent(isoKey)}`); }
  bulkDeleteCountries(isoKeys: string[]): Observable<void> { return this.http.post<void>(`${environment.apiBaseUrl}admin/api/countries/bulk-delete`, isoKeys); }
}
