import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { CurrenciesService } from '../../../service/currency/currencies.service';
import { Currency } from '../../../model/currency/currency.model';
import { SessionService } from '../../../service/session.service';
import { LocalizedStringfieldViewComponent } from '../../../components/localized-stringfield-view/localized-stringfield-view.component';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-currency-detail',
  templateUrl: './currency-detail.component.html',
  styleUrls: ['./currency-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, LocalizedStringfieldViewComponent, InfoSectionComponent, TranslocoModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class CurrencyDetailComponent {
  private currenciesService = inject(CurrenciesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);

  lang = computed(() => this.sessionService.language());
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  currency = signal<Currency | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  canWrite = computed(() => this.permissionService.hasWritePermission('Currency'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Currency'));

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.currency();
    if (!o || !o.$info) return [];

    const allInfoKeys = Object.keys(o.$info);
    if (allInfoKeys.length === 0) return [];

    const sections: InfoSection[] = [];

    // Audit Information section
    if (o.$info['createdAt'] || o.$info['lastModifiedAt']) {
      const fields: InfoField[] = [];
      const createdAt = o.$info['createdAt'];
      if (createdAt) {
        fields.push({ label: this.transloco.translate('common.fields.createdAt'), value: this.dateTime.formatDate(createdAt), type: 'text' });
      }
      if (o.$info['lastModifiedAt']) {
        fields.push({ label: this.transloco.translate('common.fields.lastModifiedAt'), value: this.dateTime.formatDate(o.$info['lastModifiedAt']), type: 'text' });
      }
      sections.push({
        title: this.transloco.translate('common.sections.auditInformation'),
        fields
      });
    }

    // Other info fields section (excluding createdAt and lastModifiedAt)
    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt' && k !== 'lastModifiedAt');
    if (otherInfoKeys.length > 0) {
      const fields: InfoField[] = otherInfoKeys.map(key => ({
        label: this.label.formatLabel(key),
        value: typeof o.$info![key] === 'object' ? JSON.stringify(o.$info![key]) : String(o.$info![key]),
        type: 'text' as const
      }));
      sections.push({
        title: this.transloco.translate('common.sections.otherInformation'),
        fields
      });
    }

    return sections;
  });

  getLocalizedName(name: { [key: string]: string } | undefined): string {
    if (!name) return '-';
    const lang = this.sessionService.language();
    return name[lang] || name['en'] || Object.values(name)[0] || '-';
  }

  constructor() {
    this.route.params.subscribe(params => {
      const currencyKey = params['currencyKey'];
      this.loadCurrency(currencyKey);
    });
  }

  private loadCurrency(currencyKey: string): void {
    this.currenciesService.getCurrency(currencyKey).subscribe({
      next: (currency) => this.currency.set(currency),
      error: (error) => {
        this.error.set('Currency not found');
        console.error('Error loading currency:', error);
      }
    });
  }

  deleteCurrency(): void {
    if (!this.canDelete()) return;

    const currency = this.currency();
    if (!currency) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.currenciesService.deleteCurrency(currency.currencyKey).subscribe({
        next: () => {
          this.router.navigate(['/' + this.lang(), 'currencies']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.currency.deleteError'));
          console.error('Error deleting currency:', error);
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    // Ensure keyboard event and guard against input/textarea targets
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (
      !this.currency() ||
      (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))
    ) {
      return;
    }

    // Show visual feedback
    this.showEditKeyHint.set(true);

    // Navigate to edit page
    const currency = this.currency();
    if (currency && this.canWrite()) {
      this.router.navigate(['/' + this.lang(), 'currencies', currency.currencyKey, 'edit']);
    }
  }
}
