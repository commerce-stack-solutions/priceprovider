import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { PriceRowList } from '../model/pricerow/price-row.model';

export class PricerowsService {
  private http = inject(HttpClient);

  getPriceRows(page: number, pageSize: number) {
    const url = `${environment.apiBaseUrl}admin/api/pricerows?page=${page}&page-size=${pageSize}`;
    return this.http.get<PriceRowList>(url);
  }
}
