import { Component, signal, inject, computed } from '@angular/core';

import { RouterModule } from '@angular/router';
import { ChannelsService } from '../../service/channel/channels.service';
import { Channel } from '../../model/channel/channel.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { ColumnFilterComponent } from '../../components/column-filter/column-filter.component';
import { FilterDefinition, ColumnFilterConfig, buildQueryString, parseQueryString } from '../../model/column-filter.model';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-channels',
  templateUrl: './channels.component.html',
  styleUrls: ['./channels.component.scss'],
  standalone: true,
  imports: [RouterModule, TranslocoModule, ColumnFilterComponent]
})
export class ChannelsComponent {
  private channelsService = inject(ChannelsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  channels = signal<Channel[]>([]);
  page = signal(0);
  pageSize = signal(50);
  totalItems = signal(0);
  totalPages = signal(0);
  sortBy = signal<string[]>([]);
  sortDirection = signal<string>('asc');
  selectedChannels = signal<Set<string>>(new Set());
  deleteError = signal<string | null>(null);

  activeFilters = signal<Map<string, FilterDefinition>>(new Map());

  filterConfigs: ColumnFilterConfig[] = [
    { field: 'id', type: 'string', label: 'ID' },
    { field: 'allowedCountryRefs', type: 'collection', label: 'Allowed Countries' },
    { field: 'priceRepresentationMode', type: 'string', label: 'Price Representation Mode' }
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

      this.loadChannels();
    });
  }

  loadChannels(): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.channelsService.getChannels(this.page(), this.pageSize(), this.sortBy(), this.sortDirection(), queryString)
      .subscribe({
        next: (data: any) => {
          this.channels.set(data.items ?? []);
          const info = data.$info?.paging;
          this.totalItems.set(info?.['total-items'] ?? 0);
          this.totalPages.set(info?.['total-pages'] ?? 0);
        },
        error: (err: any) => { this.channels.set([]); }
      });
  }

  setPage(page: number): void {
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.router.navigate([], { relativeTo: this.route, queryParams: { page, pageSize: this.pageSize(), 'sort-by': this.sortBy(), 'sort-direction': this.sortDirection(), q: queryString || undefined }, queryParamsHandling: 'merge' });
  }

  nextPage(): void { if (this.page() < this.totalPages() - 1) this.setPage(this.page() + 1); }
  previousPage(): void { if (this.page() > 0) this.setPage(this.page() - 1); }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 0; i < this.totalPages(); i++) pages.push(i);
    return pages;
  }

  sortByField(field: string): void {
    const currentSortBy = this.sortBy();
    let newDirection = 'asc';
    if (currentSortBy.includes(field)) newDirection = this.sortDirection() === 'asc' ? 'desc' : 'asc';
    const queryString = buildQueryString(Array.from(this.activeFilters().values()));
    this.router.navigate([], { relativeTo: this.route, queryParams: { 'sort-by': [field], 'sort-direction': newDirection, q: queryString || undefined }, queryParamsHandling: 'merge' });
  }

  getSortIndicator(field: string): string {
    if (this.sortBy().includes(field)) return this.sortDirection() === 'asc' ? ' ▲' : ' ▼';
    return '';
  }

  onFilterApplied(filter: FilterDefinition): void {
    const filters = new Map(this.activeFilters());
    filters.set(filter.field, filter);
    this.activeFilters.set(filters);
    const queryString = buildQueryString(Array.from(filters.values()));
    this.router.navigate([], { relativeTo: this.route, queryParams: { page: 0, q: queryString || undefined }, queryParamsHandling: 'merge' });
  }

  onFilterRemoved(field: string): void {
    const filters = new Map(this.activeFilters());
    filters.delete(field);
    this.activeFilters.set(filters);
    const queryString = buildQueryString(Array.from(filters.values()));
    this.router.navigate([], { relativeTo: this.route, queryParams: { page: 0, q: queryString || undefined }, queryParamsHandling: 'merge' });
  }

  getActiveFilter(field: string): FilterDefinition | null { return this.activeFilters().get(field) ?? null; }
  getFilterConfig(field: string): ColumnFilterConfig | undefined { return this.filterConfigs.find(c => c.field === field); }

  toggleSelection(id: string): void {
    const selected = new Set(this.selectedChannels());
    if (selected.has(id)) selected.delete(id); else selected.add(id);
    this.selectedChannels.set(selected);
  }

  toggleAllSelection(checked: boolean): void {
    const selected = new Set<string>();
    if (checked) this.channels().forEach(channel => selected.add(channel.id));
    this.selectedChannels.set(selected);
  }

  deleteSelected(): void {
    if (confirm(this.transloco.translate('common.messages.confirmDeleteMultiple', { count: this.selectedChannels().size }))) {
      const ids = Array.from(this.selectedChannels());
      this.channelsService.bulkDeleteChannels(ids).subscribe({
        next: () => { this.selectedChannels.set(new Set()); this.deleteError.set(null); this.loadChannels(); },
        error: () => { this.deleteError.set(this.transloco.translate('common.errors.channel.deleteError')); this.loadChannels(); }
      });
    }
  }
}
