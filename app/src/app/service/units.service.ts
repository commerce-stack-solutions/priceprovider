import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { UnitList } from '../model/unit/unit.model';

export class UnitsService {
  private http = inject(HttpClient);

  getUnits(page: number, pageSize: number) {
    const url = `${environment.apiBaseUrl}admin/api/units?page=${page}&page-size=${pageSize}`;
    return this.http.get<UnitList>(url);
  }
}
