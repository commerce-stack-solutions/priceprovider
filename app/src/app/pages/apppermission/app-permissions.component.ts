import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AppPermissionsService } from '../../service/approle/app-permission.service';
import { AppPermission } from '../../model/approle/app-permission.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-permissions',
  templateUrl: './app-permissions.component.html',
  styleUrls: ['./app-permissions.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class AppPermissionsComponent {
  private appPermissionsService = inject(AppPermissionsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private transloco = inject(TranslocoService);
  sessionService = inject(SessionService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  // Permission helpers for template bindings
  canWrite = computed(() => this.permissionService.hasWritePermission('AppPermission'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('AppPermission'));

  permissions = signal<AppPermission[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedPermissions = signal<Set<number>>(new Set());
  deleteError = signal<string | null>(null);
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());

  filterConfigs: ColumnFilterConfig[] = [
    { field: 'name', type: 'string', label: 'Name' },
    { field: 'description', type: 'string', label: 'Description' }
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

      const filters = parseQueryString(queryString);
      const filterMap = new Map<string, FilterDefinition>();
      filters.forEach(f => filterMap.set(f.field, f));
      this.activeFilters.set(filterMap);

      this.loadPermissions();
    });
  }

  loadPermissions(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.appPermissionsService.getAppPermissions(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), undefined, queryString)
      .subscribe({
        next: (data: any) => {
          this.permissions.set(data.items ?? []);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
          this.selectedPermissions.set(new Set());
        },
        error: () => {
          this.permissions.set([]);
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
    if (!this.sortBy().includes(field)) return '';
    return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
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

  toggleSelection(id: number): void {
    const selected = new Set(this.selectedPermissions());
    if (selected.has(id)) {
      selected.delete(id);
    } else {
      selected.add(id);
    }
    this.selectedPermissions.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<number>();
    if (checked) {
      this.permissions().forEach(permission => selected.add(permission.id));
    }
    this.selectedPermissions.set(selected);
  }

  deleteSelected(): void {
    if (!this.canDelete() || this.selectedPermissions().size === 0) return;
    const ids = Array.from(this.selectedPermissions());
    const warning = this.transloco.translate('pages.appPermissions.deleteSelectedWarning', { count: ids.length });
    if (!confirm(warning)) return;

    this.appPermissionsService.bulkDeleteAppPermissions(ids)
      .subscribe({
        next: () => {
          this.deleteError.set(null);
          this.selectedPermissions.set(new Set());
          this.loadPermissions();
        },
        error: () => {
          this.deleteError.set(this.transloco.translate('common.errors.appPermission.deleteError'));
          this.loadPermissions();
        }
      });
  }

  deletePermission(id: number): void {
    if (!this.canDelete()) return;
    const warning = this.transloco.translate('pages.appPermissions.deleteWarning', { id });
    if (!confirm(warning)) return;

    this.appPermissionsService.deleteAppPermission(id).subscribe({
      next: () => this.loadPermissions(),
      error: () => {
        this.deleteError.set(this.transloco.translate('common.errors.appPermission.deleteError'));
        this.loadPermissions();
      }
    });
  }
}
