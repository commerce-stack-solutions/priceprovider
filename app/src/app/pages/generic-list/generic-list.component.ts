import { Component, signal, inject, OnInit, computed, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TitleCasePipe, DatePipe } from '@angular/common';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';
import { MessageTranslationService } from '../../service/message-translation.service';
import { Message } from '../../model/message.model';
import { MetaInfo, FieldMetadata } from '../../model/meta-info.model';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-generic-list',
  templateUrl: './generic-list.component.html',
  styleUrls: ['./generic-list.component.scss'],
  standalone: true,
  imports: [RouterModule, TranslocoModule, ColumnFilterComponent, TitleCasePipe, DatePipe]
})
export class GenericListComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);
  private cdr = inject(ChangeDetectorRef);

  lang = computed(() => this.sessionService.language());

  entityType = signal<string>('');

  getPermissionEntityType(plural: string): string {
    const lower = plural.toLowerCase();
    if (lower === 'currencies') return 'Currency';
    if (lower === 'channels') return 'Channel';
    if (lower === 'pricerows') return 'PriceRow';
    if (lower === 'groups') return 'Group';
    if (lower === 'units') return 'Unit';
    if (lower === 'taxclasses') return 'TaxClass';
    if (lower === 'languages') return 'Language';
    if (lower === 'countries') return 'Country';
    if (lower === 'organizations') return 'Organization';
    if (lower === 'app-roles') return 'AppRole';
    if (lower === 'app-permissions') return 'AppPermission';
    return plural.charAt(0).toUpperCase() + plural.slice(1);
  }

  hasWritePermission(): boolean {
    return this.permissionService.hasWritePermission(this.getPermissionEntityType(this.entityType()));
  }

  hasDeletePermission(): boolean {
    return this.permissionService.hasDeletePermission(this.getPermissionEntityType(this.entityType()));
  }
  items = signal<any[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedIds = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  loading = signal(true);
  meta = signal<MetaInfo | null>(null);

  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());

  // Dynamic filter configs based on meta
  filterConfigs = computed<ColumnFilterConfig[]>(() => {
    const metaInfo = this.meta();
    if (!metaInfo || !metaInfo.fields) return [];
    // Only allow filtering on String, Number or Enum fields
    return metaInfo.fields
      .filter(f => ['String', 'Number', 'Enum'].includes(f.type) && !f.readOnly)
      .map(f => ({
        field: f.name,
        type: f.type === 'Number' ? 'number' : 'string',
        label: this.getLabel(f.name)
      }));
  });

  // Columns to display (excluding relationships and collections to keep table clean)
  displayColumns = computed<FieldMetadata[]>(() => {
    const metaInfo = this.meta();
    if (!metaInfo || !metaInfo.fields) return [];
    return metaInfo.fields.filter(f =>
      !['Set<Reference>', 'Reference', 'LocalizedString'].includes(f.type)
    ).slice(0, 6); // First 6 primitive/basic columns
  });

  // Localized String Columns
  localizedColumns = computed<FieldMetadata[]>(() => {
    const metaInfo = this.meta();
    if (!metaInfo || !metaInfo.fields) return [];
    return metaInfo.fields.filter(f => f.type === 'LocalizedString');
  });

  // Identity field name
  idFieldName = computed<string>(() => {
    return this.meta()?.identityFields?.[0] || 'id';
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const type = params.get('entityType') || '';
      this.entityType.set(type);

      this.route.queryParams.subscribe(qParams => {
        const page = parseInt(qParams['page'] ?? '0', 10);
        const pageSize = parseInt(qParams['pageSize'] ?? '50', 10);
        const sortBy = qParams['sort-by'] ? (Array.isArray(qParams['sort-by']) ? qParams['sort-by'] : [qParams['sort-by']]) : [];
        const sortDirection = qParams['sort-direction'] ?? 'asc';
        const queryString = qParams['q'] ?? '';

        this.page.set(isNaN(page) ? 0 : page);
        this.pageSize.set(isNaN(pageSize) ? 50 : pageSize);
        this.sortBy.set(sortBy);
        this.sortDirection.set(sortDirection);

        const filters = parseQueryString(queryString);
        const filterMap = new Map<string, FilterDefinition>();
        filters.forEach(f => filterMap.set(f.field, f));
        this.activeFilters.set(filterMap);

        this.loadMetaAndData();
      });
    });
  }

  private loadMetaAndData(): void {
    this.loading.set(true);
    const metaUrl = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/$meta`;
    this.http.get<MetaInfo>(metaUrl).subscribe({
      next: (metaInfo) => {
        this.meta.set(metaInfo);
        this.loadItems();
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  loadItems(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    let params = new HttpParams()
      .set('page', this.page().toString())
      .set('page-size', this.pageSize().toString());

    if (this.sortBy().length > 0) {
      this.sortBy().forEach(field => {
        params = params.append('sort-by', field);
      });
      if (this.sortDirection()) {
        params = params.set('sort-direction', this.sortDirection());
      }
    }

    if (queryString) {
      params = params.set('q', queryString);
    }

    const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}`;
    this.http.get<any>(url, { params }).subscribe({
      next: (data) => {
        this.items.set(data.items ?? []);
        const info = data.$info?.paging;
        this.totalItems.set(info?.['total-items'] ?? 0);
        this.totalPages.set(info?.['total-pages'] ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.items.set([]);
        this.loading.set(false);
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
    return this.filterConfigs().find(c => c.field === field);
  }

  getSortIndicator(field: string): string {
    if (this.sortBy().includes(field)) {
      return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
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

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<string>();
    const idField = this.idFieldName();
    if (checked) {
      this.items().forEach(item => {
        if (item[idField]) {
          selected.add(item[idField]);
        }
      });
    }
    this.selectedIds.set(selected);
  }

  deleteSelected(): void {
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedIds().size }))) {
      const ids = Array.from(this.selectedIds());
      const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/bulk-delete`;

      this.http.post<void>(url, ids).subscribe({
        next: () => {
          this.selectedIds.set(new Set());
          this.deleteError.set(null);
          this.loadItems();
        },
        error: (error) => {
          this.deleteError.set('Failed to delete selected items.');
          this.loadItems();
        }
      });
    }
  }

  getLocalizedValue(item: any, fieldName: string): string {
    const currentLang = this.sessionService.language();
    const localizedMap = item[fieldName];
    if (!localizedMap) return '-';
    if (localizedMap[currentLang]) {
      return localizedMap[currentLang];
    }
    if (localizedMap['en']) {
      return localizedMap['en'];
    }
    const firstKey = Object.keys(localizedMap)[0];
    if (firstKey) {
      return localizedMap[firstKey];
    }
    return '-';
  }

  getLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
  }

  getItemId(item: any): string {
    return item[this.idFieldName()];
  }
}
