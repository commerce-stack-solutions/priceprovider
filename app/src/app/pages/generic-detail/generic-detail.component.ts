import { Component, signal, inject, computed, OnInit } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { TitleCasePipe, DatePipe, CommonModule } from '@angular/common';
import { SessionService } from '../../service/session.service';
import { LocalizedStringfieldViewComponent } from '../../components/localized-stringfield-view/localized-stringfield-view.component';
import { InfoSectionComponent, InfoSection, InfoField } from '../../components/info-section/info-section.component';
import { DateTimeService } from '../../service/datetime.service';
import { LabelService } from '../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../service/permission.service';
import { MetaInfo, FieldMetadata } from '../../model/meta-info.model';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-generic-detail',
  templateUrl: './generic-detail.component.html',
  styleUrls: ['./generic-detail.component.scss'],
  standalone: true,
  imports: [RouterModule, LocalizedStringfieldViewComponent, InfoSectionComponent, TranslocoModule, TitleCasePipe, DatePipe, CommonModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class GenericDetailComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  entityType = signal<string>('');
  id = signal<string | null>(null);
  entity = signal<any | null>(null);
  meta = signal<MetaInfo | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

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

  canWrite = computed(() => this.permissionService.hasWritePermission(this.getPermissionEntityType(this.entityType())));
  canDelete = computed(() => this.permissionService.hasDeletePermission(this.getPermissionEntityType(this.entityType())));

  // Columns to display (basic fields excluding relationship objects or localized maps)
  basicFields = computed<FieldMetadata[]>(() => {
    const metaInfo = this.meta();
    if (!metaInfo || !metaInfo.fields) return [];
    return metaInfo.fields.filter(f =>
      !['Set<Reference>', 'Reference', 'LocalizedString'].includes(f.type)
    );
  });

  // Localized String fields
  localizedFields = computed<FieldMetadata[]>(() => {
    const metaInfo = this.meta();
    if (!metaInfo || !metaInfo.fields) return [];
    return metaInfo.fields.filter(f => f.type === 'LocalizedString');
  });

  // Relationship reference fields
  referenceFields = computed<FieldMetadata[]>(() => {
    const metaInfo = this.meta();
    if (!metaInfo || !metaInfo.fields) return [];
    return metaInfo.fields.filter(f => ['Reference', 'Set<Reference>'].includes(f.type));
  });

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.entity();
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

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const type = params.get('entityType') || '';
      const id = params.get('id');
      this.entityType.set(type);
      this.id.set(id);

      this.loadMetaAndData(id!);
    });
  }

  private loadMetaAndData(id: string): void {
    const metaUrl = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/$meta`;
    this.http.get<MetaInfo>(metaUrl).subscribe({
      next: (metaInfo) => {
        this.meta.set(metaInfo);
        this.loadEntity(id);
      },
      error: () => {
        this.error.set('Entity metadata not found');
      }
    });
  }

  private loadEntity(id: string): void {
    const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/${encodeURIComponent(id)}`;
    const params = new HttpParams().set('$expand', '$includes,$info,$meta');

    this.http.get<any>(url, { params }).subscribe({
      next: (data) => {
        this.entity.set(data);
      },
      error: (err) => {
        this.error.set('Entity not found');
        console.error('Error loading entity:', err);
      }
    });
  }

  getLocalizedName(name: { [key: string]: string } | undefined): string {
    if (!name) return '-';
    const lang = this.sessionService.language();
    return name[lang] || name['en'] || Object.values(name)[0] || '-';
  }

  deleteEntity(): void {
    if (!this.canDelete() || !this.id()) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/${encodeURIComponent(this.id()!)}`;
      this.http.delete<void>(url).subscribe({
        next: () => {
          this.router.navigate(['/' + this.lang(), 'generic', this.entityType().toLowerCase()]);
        },
        error: (error) => {
          this.error.set('Failed to delete entity.');
          console.error('Error deleting entity:', error);
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (
      !this.entity() ||
      (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))
    ) {
      return;
    }

    this.showEditKeyHint.set(true);

    if (this.canWrite()) {
      this.router.navigate(['/' + this.lang(), 'generic', this.entityType().toLowerCase(), this.id(), 'edit']);
    }
  }

  getLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
  }

  getDisplayValue(val: any): string {
    if (val === null || val === undefined) return '-';
    if (typeof val === 'object') return JSON.stringify(val);
    return String(val);
  }
}
