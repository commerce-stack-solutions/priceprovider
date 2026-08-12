
import { Component, signal, inject, computed } from '@angular/core';

import { RouterModule } from '@angular/router';
import { UnitsService } from '../../service/unit/units.service';
import { Unit, UnitList } from '../../model/unit/unit.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PlainNumberPipe } from '../../shared/pipes/plain-number.pipe';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-units',
  templateUrl: './units.component.html',
  styleUrls: ['./units.component.scss'],
  standalone: true,
  imports: [RouterModule, TranslocoModule, PlainNumberPipe, ColumnFilterComponent]
})
export class UnitsComponent {
  private unitsService = inject(UnitsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  units = signal<Unit[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedUnits = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  
  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());
  
  // Filter configurations
  filterConfigs: ColumnFilterConfig[] = [
    { field: 'symbol', type: 'string', label: 'Symbol' },
    { field: 'measure', type: 'string', label: 'Measure' },
    { field: 'baseUnitRef', type: 'reference', label: 'Base Unit' },
    { field: 'factor', type: 'number', label: 'Factor' }
  ];

  constructor() {
    this.route.queryParams.subscribe(params => {
      const page = parseInt(params['page'] ?? '0', 10);
      const pageSize = parseInt(params['pageSize'] ?? '50', 10);
      const sortBy = params['sort-by'] ? (Array.isArray(params['sort-by']) ? params['sort-by'] : [params['sort-by']]) : [];
      const sortDirection = params['sort-direction'] ?? 'asc';
      const queryString = params['q'] ?? '';
      
      this.page.set(isNaN(page) ? 0 : page);
      this.pageSize.set(isNaN(pageSize) ? 50 : pageSize);
      this.sortBy.set(sortBy);
      this.sortDirection.set(sortDirection);
      
      // Parse filters from query string
      const filters = parseQueryString(queryString);
      const filterMap = new Map<string, FilterDefinition>();
      filters.forEach(f => filterMap.set(f.field, f));
      this.activeFilters.set(filterMap);
      
      this.loadUnits();
    });
  }

  loadUnits(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.unitsService.getUnits(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), queryString)
      .subscribe({
        next: (data: any) => {
          this.units.set(data.items ?? []);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
        },
        error: (err: any) => {
          this.units.set([]);
        }
      });
  }

  setPage(page: number): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page, pageSize: this.pageSize(), 'sort-by': this.sortBy(), 'sort-direction': this.sortDirection(), q: queryString || undefined },
      queryParamsHandling: 'merge',
    });
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.setPage(this.page() + 1);
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.setPage(this.page() - 1);
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const totalPages = this.totalPages();
    const currentPage = this.page();
    
    // Show up to 5 page numbers around current page
    let start = Math.max(0, currentPage - 2);
    let end = Math.min(totalPages, start + 5);
    
    // Adjust start if we're near the end
    if (end - start < 5) {
      start = Math.max(0, end - 5);
    }
    
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  sortByField(field: string): void {
    let newSortBy = [field];
    let newDirection = 'asc';
    
    // Toggle direction if already sorting by this field
    if (this.sortBy().length === 1 && this.sortBy()[0] === field) {
      newDirection = this.sortDirection() === 'asc' ? 'desc' : 'asc';
    }
    
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: 0, pageSize: this.pageSize(), 'sort-by': newSortBy, 'sort-direction': newDirection, q: queryString || undefined },
      queryParamsHandling: 'merge',
    });
  }
  
  onFilterApplied(filter: FilterDefinition): void {
    const filters = new Map(this.activeFilters());
    filters.set(filter.field, filter);
    this.activeFilters.set(filters);
    
    const queryString = buildQueryString(Array.from(filters.values()));
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: 0, q: queryString || undefined },
      queryParamsHandling: 'merge',
    });
  }
  
  onFilterRemoved(field: string): void {
    const filters = new Map(this.activeFilters());
    filters.delete(field);
    this.activeFilters.set(filters);
    
    const queryString = buildQueryString(Array.from(filters.values()));
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: 0, q: queryString || undefined },
      queryParamsHandling: 'merge',
    });
  }
  
  getActiveFilter(field: string): FilterDefinition | null {
    return this.activeFilters().get(field) ?? null;
  }
  
  getFilterConfig(field: string): ColumnFilterConfig | undefined {
    return this.filterConfigs.find(c => c.field === field);
  }

  getSortIndicator(field: string): string {
    if (this.sortBy().length === 1 && this.sortBy()[0] === field) {
      return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
  }

  getLocalizedName(unit: Unit): string {
    const lang = this.sessionService.language();
    return unit.name[lang] || unit.name['en'] || '-';
  }

  toggleSelection(symbol: string): void {
    const selected = new Set(this.selectedUnits());
    if (selected.has(symbol)) {
      selected.delete(symbol);
    } else {
      selected.add(symbol);
    }
    this.selectedUnits.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<string>();
    if (checked) {
      this.units().forEach(unit => selected.add(unit.symbol));
    }
    this.selectedUnits.set(selected);
  }

  deleteSelected(): void {
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedUnits().size }))) {
      const symbols = Array.from(this.selectedUnits());
      
      this.unitsService.bulkDeleteUnits(symbols)
        .subscribe({
          next: () => {
            this.selectedUnits.set(new Set());
            this.deleteError.set(null);
            this.loadUnits();
          },
          error: (error) => {
            this.deleteError.set(this.transloco.translate('common.errors.unit.deleteError'));
            this.loadUnits();
          }
        });
    }
  }
}