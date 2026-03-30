import { Component, signal, inject, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UnitsService } from '../../../service/unit/units.service';
import { Unit } from '../../../model/unit/unit.model';
import { SessionService } from '../../../service/session.service';
import { LocalizedStringfieldViewComponent } from '../../../components/localized-stringfield-view/localized-stringfield-view.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PlainNumberPipe } from '../../../shared/pipes/plain-number.pipe';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-unit-detail',
  templateUrl: './unit-detail.component.html',
  styleUrls: ['./unit-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, LocalizedStringfieldViewComponent, InfoSectionComponent, TranslocoModule, PlainNumberPipe],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class UnitDetailComponent implements OnInit {
  private unitsService = inject(UnitsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);

  lang = computed(() => this.sessionService.language());
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);

  unit = signal<Unit | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  // Permission helpers
  protected permissionService = inject(PermissionService);
  canWrite = computed(() => this.permissionService.hasWritePermission('Unit'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Unit'));

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.unit();
    if (!o) return [];
    const allInfoKeys = Object.keys(o.$info ?? {});
    if (allInfoKeys.length === 0) return [];
    const sections: InfoSection[] = [];

    // Base unit details (from $includes)
    if (o.$includes?.baseUnit) {
      sections.push({
        title: this.transloco.translate('common.sections.baseUnitDetails'),
        fields: [
          { label: this.transloco.translate('common.fields.symbol'), value: o.$includes.baseUnit.symbol, type: 'text' },
          { label: this.transloco.translate('common.fields.name'), value: o.$includes.baseUnit.name, type: 'localized' },
          { label: this.transloco.translate('common.fields.measure'), value: o.$includes.baseUnit.measure, type: 'text' }
        ]
      });
    }

    // Add Audit Information section if available
    if (o.$info && (o.$info['createdAt'] || o.$info['lastModifiedAt'])) {
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

  ngOnInit(): void {
    const symbol = this.route.snapshot.paramMap.get('symbol');
    if (symbol) {
      this.loadUnit(symbol);
    } else {
      this.error.set('Unit symbol not provided');
      this.loading.set(false);
    }
  }

  loadUnit(symbol: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.unitsService.getUnit(symbol).subscribe({
      next: (data: Unit) => {
        this.unit.set(data);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load unit: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  navigateToEdit(): void {
    if (!this.canWrite()) return;
    const symbol = this.unit()?.symbol;
    if (symbol) {
      this.router.navigate(['/' + this.lang(), 'units', symbol, 'edit']);
    }
  }

  navigateToList(): void {
    this.router.navigate(['/' + this.lang(), 'units']);
  }

  deleteUnit(): void {
    if (!this.canDelete()) return;
    const unit = this.unit();
    if (!unit) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.unitsService.deleteUnit(unit.symbol).subscribe({
        next: () => {
          this.router.navigate(['/' + this.lang(), 'units']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.unit.deleteError'));
          console.error('Error deleting unit:', error);
        }
      });
    }
  }

  getLocalizedName(name: { [key: string]: string } | undefined): string {
    if (!name) return '-';
    const lang = this.sessionService.language();
    return name[lang] || name['en'] || Object.values(name)[0] || '-';
  }

  handleEditKeyPress(event: Event): void {
    // Only handle if unit is loaded and not already on an input field
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.unit() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) {
      return;
    }

    // Show visual feedback
    this.showEditKeyHint.set(true);

    // Navigate to edit page
    const unit = this.unit();
    if (unit) {
      this.router.navigate(['/' + this.lang(), 'units', unit.symbol, 'edit']);
    }
  }
}
