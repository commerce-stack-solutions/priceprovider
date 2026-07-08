import { inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../environments/environment';
import { Observable } from 'rxjs';

export interface DataFilesPreview {
  essentialFiles: string[];
  sampleFiles: string[];
  essentialDataDirectory: string;
  sampleDataDirectory: string;
}

export interface InitializationResponse {
  status: string;
  message: string;
}

export class ServiceInitializationService {
  private http = inject(HttpClient);

  getDataFilesPreview(): Observable<DataFilesPreview> {
    const url = `${environment.apiBaseUrl}admin/api/service-initialization/preview`;
    return this.http.get<DataFilesPreview>(url);
  }

  loadData(loadEssential: boolean, loadSample: boolean): Observable<InitializationResponse> {
    const url = `${environment.apiBaseUrl}admin/api/service-initialization/load`;
    const params = new HttpParams()
      .set('essential', loadEssential.toString())
      .set('sample', loadSample.toString());
    return this.http.post<InitializationResponse>(url, {}, { params });
  }
}
