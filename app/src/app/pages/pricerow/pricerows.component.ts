
import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PricerowsService } from '../../service/pricerow/pricerows.service';
import { PriceRow, PriceRowList } from '../../model/pricerow/price-row.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-pricerows',
  templateUrl: './pricerows.component.html',
  styleUrls: ['./pricerows.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class PriceRowsComponent {
  private pricerowsService = new PricerowsService();
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  page = signal(0);
  pageSize = signal(50);
  priceRows = signal<PriceRow[]>([]);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedPriceRows = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  
  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());
  
  // Filter configurations
  filterConfigs: ColumnFilterConfig[] = [
    { field: 'id', type: 'string', label: 'ID' },
    { field: 'pricedResourceId', type: 'string', label: 'Resource ID' },
    { field: 'priceValue', type: 'number', label: 'Price Value' },
    { field: 'minQuantity', type: 'number', label: 'Min Quantity' },
    { field: 'unitRef', type: 'reference', label: 'Unit' },
    { field: 'currencyRef', type: 'reference', label: 'Currency' },
    { field: 'taxClassRef', type: 'reference', label: 'Tax Class' },
    { field: 'taxIncluded', type: 'boolean', label: 'Tax Included' },
    { field: 'validFrom', type: 'datetime', label: 'Valid From' },
    { field: 'validTo', type: 'datetime', label: 'Valid To' },
    { field: 'groupRefs', type: 'collection', label: 'Groups' },
    { field: 'channelRefs', type: 'collection', label: 'Channels' }
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
      
      this.loadPriceRows();
    });
  }

  loadPriceRows(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.pricerowsService.getPriceRows(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), queryString)
      .subscribe({
        next: (data: any) => {
          this.priceRows.set(data.items);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
        },
        error: (err: any) => {
          this.priceRows.set([]);
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

  getSortIndicator(field: string): string {
    if (this.sortBy().length === 1 && this.sortBy()[0] === field) {
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

  formatDate(date: string | undefined): string {
    if (!date) return '-';
    const dateObj = new Date(date);
    const locale = this.sessionService.language();
    return dateObj.toLocaleDateString(locale, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone: 'Europe/Berlin'
    });
  }

  copiedId = signal<string | null>(null);

  copyIdToClipboard(id: string, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    navigator.clipboard.writeText(id).then(() => {
      this.copiedId.set(id);
      setTimeout(() => this.copiedId.set(null), 1500);
    });
  }

  shortId(id: string): string {
    return id ? id.substring(0, 5) + '…' : '';
  }

  toggleSelection(id: string): void {
    const selected = new Set(this.selectedPriceRows());
    if (selected.has(id)) {
      selected.delete(id);
    } else {
      selected.add(id);
    }
    this.selectedPriceRows.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<string>();
    if (checked) {
      this.priceRows().forEach(row => selected.add(row.id));
    }
    this.selectedPriceRows.set(selected);
  }

  deleteSelected(): void {
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedPriceRows().size }))) {
      const ids = Array.from(this.selectedPriceRows());
      
      this.pricerowsService.bulkDeletePriceRows(ids)
        .subscribe({
          next: () => {
            this.selectedPriceRows.set(new Set());
            this.deleteError.set(null);
            this.loadPriceRows();
          },
          error: (error) => {
            this.deleteError.set(this.transloco.translate('common.errors.priceRow.deleteError'));
            this.loadPriceRows();
          }
        });
    }
  }
}