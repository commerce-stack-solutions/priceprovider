import { Component, signal, inject, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PricerowsService } from '../../../service/pricerow/pricerows.service';
import { PriceRow } from '../../../model/pricerow/price-row.model';
import { SessionService } from '../../../service/session.service';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-pricerow-detail',
  templateUrl: './pricerow-detail.component.html',
  styleUrls: ['./pricerow-detail.component.scss'],
  imports: [CommonModule, RouterModule, InfoSectionComponent, TranslocoModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class PricerowDetailComponent implements OnInit {
  private pricerowsService = new PricerowsService();
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);

  lang = computed(() => this.sessionService.language());
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  canWrite = computed(() => this.permissionService.hasWritePermission('PriceRow'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('PriceRow'));

  priceRow = signal<PriceRow | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.priceRow();
    if (!o) return [];

    const allInfoKeys = o.$info ? Object.keys(o.$info) : [];
    if (allInfoKeys.length === 0) return [];

    const sections: InfoSection[] = [];

    // Taxation info
    if (o.$info?.taxation) {
      sections.push({
        title: this.transloco.translate('common.sections.taxationDetails'),
        fields: [
          { label: this.transloco.translate('common.fields.taxRate'), value: o.$info.taxation.taxRate, type: 'percentage' },
          { label: this.transloco.translate('common.fields.taxValue'), value: o.$info.taxation.taxValue, type: 'currency', currencyCode: o.currencyRef },
          { label: this.transloco.translate('common.fields.taxStatus'), value: o.$info.taxation.taxIncludedInfo, type: 'text' }
        ]
      });
    }

    // Unit details
    if (o.$includes?.unit) {
      const fields: InfoField[] = [
        { label: this.transloco.translate('common.fields.symbol'), value: o.$includes.unit.symbol, type: 'text' },
        { label: this.transloco.translate('common.fields.name'), value: o.$includes.unit.name, type: 'localized' },
        { label: this.transloco.translate('common.fields.measure'), value: o.$includes.unit.measure, type: 'text' }
      ];

      if (o.$includes.unit.baseUnitRef) {
        fields.push({ label: this.transloco.translate('common.fields.baseUnit'), value: o.$includes.unit.baseUnitRef, type: 'text' });
        fields.push({ label: this.transloco.translate('common.fields.factor'), value: o.$includes.unit.factor, type: 'text' });
      }

      sections.push({
        title: this.transloco.translate('common.sections.unitDetails'),
        fields
      });
    }

    // Currency details
    if (o.$includes?.currency) {
      sections.push({
        title: this.transloco.translate('common.sections.currencyDetails'),
        fields: [
          { label: this.transloco.translate('common.fields.code'), value: o.$includes.currency.currencyKey, type: 'text' },
          { label: this.transloco.translate('common.fields.symbol'), value: o.$includes.currency.symbol, type: 'text' },
          { label: this.transloco.translate('common.fields.name'), value: o.$includes.currency.name, type: 'localized' }
        ]
      });
    }

    // Tax class details
    if (o.$includes?.taxClass) {
      const fields: InfoField[] = [
        { label: this.transloco.translate('common.fields.taxKey'), value: o.$includes.taxClass.taxClassId, type: 'text' },
        { label: this.transloco.translate('common.fields.taxRate'), value: o.$includes.taxClass.taxRate, type: 'percentage' }
      ];

      if (o.$includes.taxClass.description) {
        fields.push({ label: this.transloco.translate('common.fields.description'), value: o.$includes.taxClass.description, type: 'localized' });
      }

      sections.push({
        title: this.transloco.translate('common.sections.taxClassDetails'),
        fields
      });
    }

    // Add Audit Information section if available
    // Audit Information section
    if (o.$info?.['createdAt'] || o.$info?.['lastModifiedAt']) {
      const fields: InfoField[] = [];
      const createdAt = o.$info?.['createdAt'];
      if (createdAt) {
        fields.push({ label: this.transloco.translate('common.fields.createdAt'), value: this.dateTime.formatDate(createdAt), type: 'text' });
      }
      if (o.$info?.['lastModifiedAt']) {
        fields.push({ label: this.transloco.translate('common.fields.lastModifiedAt'), value: this.dateTime.formatDate(o.$info?.['lastModifiedAt']), type: 'text' });
      }
      sections.push({
        title: this.transloco.translate('common.sections.auditInformation'),
        fields
      });
    }

    // Other info fields section (excluding audit fields, taxation, and navigation maps already shown as badges)
    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt' && k !== 'lastModifiedAt' && k !== 'taxation' && k !== 'groupRefIds');
    if (otherInfoKeys.length > 0) {
      const fields: InfoField[] = otherInfoKeys.map(key => ({
        label: this.label.formatLabel(key),
        value: typeof (o.$info as any)[key] === 'object' ? JSON.stringify((o.$info as any)[key]) : String((o.$info as any)[key]),
        type: 'text' as const
      }));
      sections.push({
        title: this.transloco.translate('common.sections.otherInformation'),
        fields
      });
    }

    return sections;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPriceRow(parseInt(id, 10));
    } else {
      this.error.set('Price row ID not provided');
      this.loading.set(false);
    }
  }

  loadPriceRow(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.pricerowsService.getPriceRow(id).subscribe({
      next: (data: PriceRow) => {
        this.priceRow.set(data);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load price row: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  navigateToEdit(): void {
    if (!this.canWrite()) return;
    const id = this.priceRow()?.id;
    if (id) {
      this.router.navigate(['/' + this.lang(), 'pricerows', id, 'edit']);
    }
  }

  navigateToList(): void {
    this.router.navigate(['/' + this.lang(), 'pricerows']);
  }

  deletePriceRow(): void {
    if (!this.canDelete()) return;

    const priceRow = this.priceRow();
    if (!priceRow) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.pricerowsService.deletePriceRow(priceRow.id).subscribe({
        next: () => {
          this.router.navigate(['/pricerows']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.priceRow.deleteError'));
          console.error('Error deleting price row:', error);
        }
      });
    }
  }

  formatDate(date: string | undefined): string {
    if (!date) return '-';
    const dateObj = new Date(date);
    const locale = this.sessionService.language();
    // Use Europe/Berlin as the default timezone to ensure consistency
    return dateObj.toLocaleString(locale, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      timeZone: 'Europe/Berlin',
      timeZoneName: 'short'
    });
  }

  handleEditKeyPress(event: Event): void {
    // Only handle if priceRow is loaded and not already on an input field
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.priceRow() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) {
      return;
    }

    // Show visual feedback
    this.showEditKeyHint.set(true);

    // Navigate to edit page
    const priceRow = this.priceRow();
    if (priceRow && priceRow.id && this.canWrite()) {
      this.router.navigate(['/' + this.lang(), 'pricerows', priceRow.id, 'edit']);
    }
  }
}
