import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { GroupsService } from '../../service/group/groups.service';
import { Group } from '../../model/group/group.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-groups',
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class GroupsComponent {
  private groupsService = inject(GroupsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());
  
  groups = signal<Group[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedGroups = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  
  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());
  
  // Filter configurations
  filterConfigs: ColumnFilterConfig[] = [
    { field: 'path', type: 'string', label: 'Path' },
    { field: 'name', type: 'string', label: 'Name' },
    { field: 'parentRefs', type: 'collection', label: 'Parents' },
    { field: 'subRefs', type: 'collection', label: 'Sub Groups' }
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
      
      this.loadGroups();
    });
  }

  loadGroups(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.groupsService.getGroups(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), undefined, queryString)
      .subscribe({
        next: (data: any) => {
          this.groups.set(data.items ?? []);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
        },
        error: (err: any) => {
          this.groups.set([]);
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
    if (!this.sortBy().includes(field)) {
      return '';
    }
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

  toggleSelection(id: string): void {
    const selected = new Set(this.selectedGroups());
    if (selected.has(id)) {
      selected.delete(id);
    } else {
      selected.add(id);
    }
    this.selectedGroups.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    if (checked) {
      const allIds = this.groups().filter(g => g.id).map(g => g.id as string);
      this.selectedGroups.set(new Set(allIds));
    } else {
      this.selectedGroups.set(new Set());
    }
  }

  deleteSelected(): void {
    if (this.selectedGroups().size === 0) return;
    
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedGroups().size }))) {
      const ids = Array.from(this.selectedGroups()).filter(id => id !== undefined) as string[];
      this.groupsService.bulkDeleteGroups(ids).subscribe({
        next: () => {
          this.selectedGroups.set(new Set());
          this.deleteError.set(null);
          this.loadGroups();
        },
        error: (err: any) => {
          this.deleteError.set(this.transloco.translate('common.errors.group.deleteError'));
          this.loadGroups();
        }
      });
    }
  }
}
