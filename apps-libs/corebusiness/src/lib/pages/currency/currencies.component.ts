
import { Component, signal, inject, computed } from '@angular/core';

import { RouterModule } from '@angular/router';
import { CurrenciesService } from '../../service/currency/currencies.service';
import { Currency } from '../../model/currency/currency.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';
import { MessageTranslationService } from '../../service/message-translation.service';
import { Message } from '../../model/message.model';

@Component({
  selector: 'app-currencies',
  templateUrl: './currencies.component.html',
  styleUrls: ['./currencies.component.scss'],
  standalone: true,
  imports: [RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class CurrenciesComponent {
  private currenciesService = inject(CurrenciesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  currencies = signal<Currency[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedCurrencies = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);
  
  // Filter state
  activeFilters = signal<Map<string, FilterDefinition>>(new Map());
  
  // Filter configurations
  filterConfigs: ColumnFilterConfig[] = [
    { field: 'currencyKey', type: 'string', label: 'Currency Key' },
    { field: 'symbol', type: 'string', label: 'Symbol' }
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
      
      this.loadCurrencies();
    });
  }

  loadCurrencies(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.currenciesService.getCurrencies(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), queryString)
      .subscribe({
        next: (data: any) => {
          this.currencies.set(data.items ?? []);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
        },
        error: (err: any) => {
          this.currencies.set([]);
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
    return this.filterConfigs.find(c => c.field === field);
  }

  getSortIndicator(field: string): string {
    if (this.sortBy().includes(field)) {
      return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
  }

  toggleSelection(currencyKey: string): void {
    const selected = new Set(this.selectedCurrencies());
    if (selected.has(currencyKey)) {
      selected.delete(currencyKey);
    } else {
      selected.add(currencyKey);
    }
    this.selectedCurrencies.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<string>();
    if (checked) {
      this.currencies().forEach(currency => selected.add(currency.currencyKey));
    }
    this.selectedCurrencies.set(selected);
  }

  deleteSelected(): void {
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedCurrencies().size }))) {
      const currencyKeys = Array.from(this.selectedCurrencies());
      
      this.currenciesService.bulkDeleteCurrencies(currencyKeys)
        .subscribe({
          next: () => {
            this.selectedCurrencies.set(new Set());
            this.deleteError.set(null);
            this.loadCurrencies();
          },
          error: (error) => {
            const messages: Message[] = error?.error?.$messages;
            if (messages && messages.length > 0) {
              const translated = messages
                .filter((m: Message) => m.type === 'ERROR')
                .map((m: Message) => this.messageTranslationService.translateMessage(m))
                .join(' ');
              this.deleteError.set(translated || this.transloco.translate('common.errors.currency.deleteError'));
            } else {
              this.deleteError.set(this.transloco.translate('common.errors.currency.deleteError'));
            }
            this.loadCurrencies();
          }
        });
    }
  }

  getLocalizedName(currency: Currency): string {
    const currentLang = this.sessionService.language();
    if (currency.name && currency.name[currentLang]) {
      return currency.name[currentLang];
    }
    // Fallback to English if current language is not available
    if (currency.name && currency.name['en']) {
      return currency.name['en'];
    }
    // Fallback to any available name
    if (currency.name) {
      const firstKey = Object.keys(currency.name)[0];
      if (firstKey) {
        return currency.name[firstKey];
      }
    }
    return '-';
  }
}
