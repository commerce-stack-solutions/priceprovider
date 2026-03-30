import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { AppPermissionsService } from '../../../service/approle/app-permission.service';
import { AppPermission } from '../../../model/approle/app-permission.model';
import { SessionService } from '../../../service/session.service';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-permission-detail',
  templateUrl: './app-permission-detail.component.html',
  styleUrls: ['./app-permission-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, InfoSectionComponent, TranslocoModule]
})
export class AppPermissionDetailComponent {
  private appPermissionsService = inject(AppPermissionsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);

  lang = computed(() => this.sessionService.language());

  permission = signal<AppPermission | null>(null);
  error = signal<string | null>(null);

  // Permission helpers for template bindings
  protected permissionService = inject(PermissionService);
  canWrite = computed(() => this.permissionService.hasWritePermission('AppPermission'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('AppPermission'));

  infoSections = computed<InfoSection[]>(() => {
    const o = this.permission();
    if (!o || !o.$info) return [];

    const allInfoKeys = Object.keys(o.$info);
    if (allInfoKeys.length === 0) return [];

    const sections: InfoSection[] = [];

    if (o.$info['createdAt'] || o.$info['lastModifiedAt']) {
      const fields: InfoField[] = [];
      if (o.$info['createdAt']) {
        fields.push({ label: this.transloco.translate('common.fields.createdAt'), value: this.dateTime.formatDate(o.$info['createdAt']), type: 'text' });
      }
      if (o.$info['lastModifiedAt']) {
        fields.push({ label: this.transloco.translate('common.fields.lastModifiedAt'), value: this.dateTime.formatDate(o.$info['lastModifiedAt']), type: 'text' });
      }
      sections.push({ title: this.transloco.translate('common.sections.auditInformation'), fields });
    }

    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt' && k !== 'lastModifiedAt');
    if (otherInfoKeys.length > 0) {
      const fields: InfoField[] = otherInfoKeys.map(key => ({
        label: this.label.formatLabel(key),
        value: typeof o.$info![key] === 'object' ? JSON.stringify(o.$info![key]) : String(o.$info![key]),
        type: 'text' as const
      }));
      sections.push({ title: this.transloco.translate('common.sections.otherInformation'), fields });
    }

    return sections;
  });

  constructor() {
    this.route.params.subscribe(params => {
      const id = params['id'];
      this.loadPermission(id);
    });
  }

  private loadPermission(id: string): void {
    this.appPermissionsService.getAppPermission(id).subscribe({
      next: (permission) => this.permission.set(permission),
      error: () => this.error.set('App Permission not found')
    });
  }

  deletePermission(): void {
    const p = this.permission();
    if (!p) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.appPermissionsService.deleteAppPermission(p.id).subscribe({
        next: () => this.router.navigate(['/' + this.lang(), 'app-permissions']),
        error: () => this.error.set(this.transloco.translate('common.errors.appPermission.deleteError'))
      });
    }
  }
}
