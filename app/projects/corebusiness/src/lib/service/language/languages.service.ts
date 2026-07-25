import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Language, LanguageList } from '../../model/language/language.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LanguagesService {
  private http = inject(HttpClient);

  getLanguages(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string) {
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
    
    const url = `${environment.apiBaseUrl}admin/api/languages`;
    return this.http.get<LanguageList>(url, { params });
  }

  getLanguage(isoKey: string): Observable<Language> {
    const url = `${environment.apiBaseUrl}admin/api/languages/${encodeURIComponent(isoKey)}`;
    const params = new HttpParams()
      .set('$expand', '$includes,$info,$meta');
    return this.http.get<Language>(url, { params });
  }

  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/languages/$meta`);
  }

  createLanguage(language: Language): Observable<Language> {
    const url = `${environment.apiBaseUrl}admin/api/languages/create`;
    return this.http.post<Language>(url, language);
  }

  updateLanguage(isoKey: string, language: Language): Observable<Language> {
    const url = `${environment.apiBaseUrl}admin/api/languages/${encodeURIComponent(isoKey)}`;
    return this.http.put<Language>(url, language);
  }

  patchLanguage(isoKey: string, patch: JsonPatchOperation[]): Observable<Language> {
    const url = `${environment.apiBaseUrl}admin/api/languages/${encodeURIComponent(isoKey)}`;
    return this.http.patch<Language>(url, patch, {
      headers: { 'Content-Type': 'application/json-patch+json' }
    });
  }

  deleteLanguage(isoKey: string): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/languages/${encodeURIComponent(isoKey)}`;
    return this.http.delete<void>(url);
  }

  bulkDeleteLanguages(isoKeys: string[]): Observable<void> {
    const url = `${environment.apiBaseUrl}admin/api/languages/bulk-delete`;
    return this.http.post<void>(url, isoKeys);
  }
}

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}
