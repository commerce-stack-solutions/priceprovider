
import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TaxClassesService } from '../../service/taxclass/taxclasses.service';
import { TaxClass } from '../../model/taxclass/taxclass.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-taxclasses',
  templateUrl: './taxclasses.component.html',
  styleUrls: ['./taxclasses.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class TaxClassesComponent {
  private taxClassesService = inject(TaxClassesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  taxClasses = signal<TaxClass[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedTaxClasses = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  
  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());
  
  // Filter configurations
  filterConfigs: ColumnFilterConfig[] = [
    { field: 'taxClassId', type: 'string', label: 'Tax Class ID' },
    { field: 'taxRate', type: 'number', label: 'Tax Rate' }
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
      
      this.loadTaxClasses();
    });
  }

  loadTaxClasses(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.taxClassesService.getTaxClasses(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), queryString)
      .subscribe({
        next: (data: any) => {
          this.taxClasses.set(data.items ?? []);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
        },
        error: (err: any) => {
          this.taxClasses.set([]);
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
    for (let i = 0; i < this.totalPages(); i++) {
      pages.push(i);
    }
    return pages;
  }

  sortByField(field: string): void {
    const currentSortBy = this.sortBy();
    let newDirection = 'asc';
    if (currentSortBy.includes(field)) {
      newDirection = this.sortDirection() === 'asc' ? 'desc' : 'asc';
    }
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { 'sort-by': [field], 'sort-direction': newDirection, q: queryString || undefined },
      queryParamsHandling: 'merge',
    });
  }

  getSortIndicator(field: string): string {
    if (this.sortBy().includes(field)) {
      return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
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

  toggleSelection(taxClassId: string): void {
    const selected = new Set(this.selectedTaxClasses());
    if (selected.has(taxClassId)) {
      selected.delete(taxClassId);
    } else {
      selected.add(taxClassId);
    }
    this.selectedTaxClasses.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<string>();
    if (checked) {
      this.taxClasses().forEach(taxClass => selected.add(taxClass.taxClassId));
    }
    this.selectedTaxClasses.set(selected);
  }

  deleteSelected(): void {
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedTaxClasses().size }))) {
      const taxClassIds = Array.from(this.selectedTaxClasses());
      
      this.taxClassesService.bulkDeleteTaxClasses(taxClassIds)
        .subscribe({
          next: () => {
            this.selectedTaxClasses.set(new Set());
            this.deleteError.set(null);
            this.loadTaxClasses();
          },
          error: (error) => {
            this.deleteError.set(this.transloco.translate('common.errors.taxClass.deleteError'));
            this.loadTaxClasses();
          }
        });
    }
  }
}
