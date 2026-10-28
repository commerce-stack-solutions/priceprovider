import { Component, signal, inject, computed } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { CountriesService } from '../../../service/country/countries.service';
import { Country } from '../../../model/country/country.model';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { LabelService } from '../../../service/label.service';
import { DateTimeService } from '../../../service/datetime.service';
import { SessionService } from '../../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-country-detail',
  templateUrl: './country-detail.component.html',
  styleUrls: ['./country-detail.component.scss'],
  standalone: true,
  imports: [KeyValuePipe, RouterModule, InfoSectionComponent, TranslocoModule],
  host: { '(document:keydown.e)': 'handleEditKeyPress($event)' }
})
export class CountryDetailComponent {
  private countriesService = inject(CountriesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  country = signal<Country | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  canWrite = computed(() => this.permissionService.hasWritePermission('Country'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Country'));

  infoSections = computed<InfoSection[]>(() => {
    const o = this.country();
    if (!o || !o.$info) return [];
    const allInfoKeys = Object.keys(o.$info);
    if (allInfoKeys.length === 0) return [];
    const sections: InfoSection[] = [];
    if (o.$info['createdAt'] || o.$info['lastModifiedAt']) {
      const fields: InfoField[] = [];
      if (o.$info['createdAt']) fields.push({ label: this.transloco.translate('common.fields.createdAt'), value: this.dateTime.formatDate(o.$info['createdAt']), type: 'text' });
      if (o.$info['lastModifiedAt']) fields.push({ label: this.transloco.translate('common.fields.lastModifiedAt'), value: this.dateTime.formatDate(o.$info['lastModifiedAt']), type: 'text' });
      sections.push({ title: this.transloco.translate('common.sections.auditInformation'), fields });
    }
    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt' && k !== 'lastModifiedAt');
    if (otherInfoKeys.length > 0) {
      const fields: InfoField[] = otherInfoKeys.map(key => ({ label: this.label.formatLabel(key), value: typeof o.$info![key] === 'object' ? JSON.stringify(o.$info![key]) : String(o.$info![key]), type: 'text' as const }));
      sections.push({ title: this.transloco.translate('common.sections.otherInformation'), fields });
    }
    return sections;
  });

  constructor() {
    this.route.params.subscribe(params => {
      const isoKey = params['isoKey'];
      this.loadCountry(isoKey);
    });
  }

  private loadCountry(isoKey: string): void {
    this.countriesService.getCountry(isoKey).subscribe({
      next: (country) => this.country.set(country),
      error: (error) => { this.error.set('Country not found'); console.error('Error loading country:', error); }
    });
  }

  deleteCountry(): void {
    if (!this.canDelete()) return;

    const country = this.country();
    if (!country) return;
    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.countriesService.deleteCountry(country.isoKey).subscribe({
        next: () => { this.router.navigate(['/' + this.lang(), 'countries']); },
        error: (error) => { this.error.set(this.transloco.translate('common.errors.country.deleteError')); console.error('Error deleting country:', error); }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.country() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) return;
    this.showEditKeyHint.set(true);
    const country = this.country();
    if (country && this.canWrite()) this.router.navigate(['/' + this.lang(), 'countries', country.isoKey, 'edit']);
  }
}
