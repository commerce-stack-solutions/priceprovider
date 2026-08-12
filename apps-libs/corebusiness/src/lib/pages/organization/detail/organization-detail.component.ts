import { Component, signal, inject, computed } from '@angular/core';

import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { OrganizationsService } from '../../../service/organization/organizations.service';
import { Organization } from '../../../model/organization/organization.model';
import { SessionService } from '../../../service/session.service';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-organization-detail',
  templateUrl: './organization-detail.component.html',
  styleUrls: ['./organization-detail.component.scss'],
  standalone: true,
  imports: [RouterModule, InfoSectionComponent, TranslocoModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class OrganizationDetailComponent {
  private organizationsService = inject(OrganizationsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);

  lang = computed(() => this.sessionService.language());
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  canWrite = computed(() => this.permissionService.hasWritePermission('Organization'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Organization'));

  organization = signal<Organization | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  infoSections = computed<InfoSection[]>(() => {
    const o = this.organization();
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

    // Other info fields section (excluding audit fields and navigation maps already shown as badges)
    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt'&& k !== 'lastModifiedAt' && k !== 'parentRefIds' && k !== 'subRefIds');
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

  // Date formatting moved to DateTimeService

  // Label formatting moved to LabelService

  constructor() {
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.loadOrganization(id);
    });
  }

  private loadOrganization(id: string): void {
    this.organizationsService.getOrganization(id).subscribe({
      next: (organization) => this.organization.set(organization),
      error: (error) => {
        this.error.set('Organization not found');
        console.error('Error loading organization:', error);
      }
    });
  }

  deleteOrganization(): void {
    if (!this.canDelete()) return;

    const org = this.organization();
    if (!org) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.organizationsService.deleteOrganization(org.id!).subscribe({
        next: () => {
          this.router.navigate(['/organizations']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.organization.deleteError'));
          console.error('Error deleting organization:', error);
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (
      !this.organization() ||
      (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))
    ) {
      return;
    }

    this.showEditKeyHint.set(true);

    const org = this.organization();
    if (org && this.canWrite()) {
      this.router.navigate(['/' + this.lang(), 'organizations', org.id, 'edit']);
    }
  }
}
