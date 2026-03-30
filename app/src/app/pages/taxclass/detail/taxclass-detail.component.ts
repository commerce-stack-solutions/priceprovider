import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { TaxClassesService } from '../../../service/taxclass/taxclasses.service';
import { TaxClass } from '../../../model/taxclass/taxclass.model';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { LabelService } from '../../../service/label.service';
import { DateTimeService } from '../../../service/datetime.service';
import { SessionService } from '../../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-taxclass-detail',
  templateUrl: './taxclass-detail.component.html',
  styleUrls: ['./taxclass-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, InfoSectionComponent, TranslocoModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class TaxClassDetailComponent {
  private taxClassesService = inject(TaxClassesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);

  lang = computed(() => this.sessionService.language());

  taxClass = signal<TaxClass | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  // Permission helpers
  protected permissionService = inject(PermissionService);
  canWrite = computed(() => this.permissionService.hasWritePermission('TaxClass'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('TaxClass'));

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.taxClass();
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

  constructor() {
    this.route.params.subscribe(params => {
      const taxClassId = params['taxClassId'];
      this.loadTaxClass(taxClassId);
    });
  }

  private loadTaxClass(taxClassId: string): void {
    this.taxClassesService.getTaxClass(taxClassId).subscribe({
      next: (taxClass) => this.taxClass.set(taxClass),
      error: (error) => {
        this.error.set('Tax class not found');
        console.error('Error loading tax class:', error);
      }
    });
  }

  deleteTaxClass(): void {
    if (!this.canDelete()) return;
    const taxClass = this.taxClass();
    if (!taxClass) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.taxClassesService.deleteTaxClass(taxClass.taxClassId).subscribe({
        next: () => {
          this.router.navigate(['/' + this.lang(), 'taxclasses']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.taxClass.deleteError'));
          console.error('Error deleting tax class:', error);
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    // Only handle if taxClass is loaded and not already on an input field
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.taxClass() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) {
      return;
    }

    // Show visual feedback
    this.showEditKeyHint.set(true);

    // Navigate to edit page
    const taxClass = this.taxClass();
    if (taxClass) {
      this.router.navigate(['/' + this.lang(), 'taxclasses', taxClass.taxClassId, 'edit']);
    }
  }
}
