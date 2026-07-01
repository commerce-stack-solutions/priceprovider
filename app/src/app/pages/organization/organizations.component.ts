import { Component, signal, inject, computed } from '@angular/core';

import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { OrganizationsService } from '../../service/organization/organizations.service';
import { Organization, OrganizationList } from '../../model/organization/organization.model';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-organizations',
  templateUrl: './organizations.component.html',
  styleUrls: ['./organizations.component.scss'],
  standalone: true,
  imports: [RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class OrganizationsComponent {
  private organizationsService = inject(OrganizationsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  organizations = signal<Organization[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  
  // Pagination
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  
  // Sorting
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  
  // Selection
  selectedIds = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  
  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());
  
  // Filter configurations
  filterConfigs: ColumnFilterConfig[] = [
    { field: 'path', type: 'string', label: 'Path' },
    { field: 'name', type: 'string', label: 'Name' },
    { field: 'organizationType', type: 'string', label: 'Organization Type' },
    { field: 'parentRefs', type: 'collection', label: 'Parents' },
    { field: 'subRefs', type: 'collection', label: 'Sub Organizations' }
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
      
      this.loadOrganizations();
    });
  }

  loadOrganizations(): void {
    this.loading.set(true);
    this.error.set(null);
    
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.organizationsService.getOrganizations(
      this.page(),
      this.pageSize(),
      this.sortBy(),
      this.sortDirection(),
      undefined,
      queryString
    ).subscribe({
      next: (list: OrganizationList) => {
        this.organizations.set(list.items || []);
        const info = list.$info?.paging;
        this.totalItems.set(info?.['total-items'] || 0);
        this.totalPages.set(info?.['total-pages'] || 0);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load organizations');
        console.error('Error loading organizations:', err);
        this.loading.set(false);
      }
    });
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

  toggleSelection(id: string): void {
    const selected = new Set(this.selectedIds());
    if (selected.has(id)) {
      selected.delete(id);
    } else {
      selected.add(id);
    }
    this.selectedIds.set(selected);
  }

  toggleSelectAll(): void {
    const selected = new Set(this.selectedIds());
    if (selected.size === this.organizations().length) {
      selected.clear();
    } else {
      this.organizations().forEach(org => { if (org.id) selected.add(org.id); });
    }
    this.selectedIds.set(selected);
  }

  bulkDelete(): void {
    const selected = Array.from(this.selectedIds());
    if (selected.length === 0) return;

    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: selected.length }))) {
      this.organizationsService.bulkDelete(selected).subscribe({
        next: () => {
          this.selectedIds.set(new Set());
          this.deleteError.set(null);
          this.loadOrganizations();
        },
        error: (err) => {
          this.deleteError.set(this.transloco.translate('common.errors.organization.deleteError'));
          console.error('Error:', err);
          this.loadOrganizations();
        }
      });
    }
  }
}
