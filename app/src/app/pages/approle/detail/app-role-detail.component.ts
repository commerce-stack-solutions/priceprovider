import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { AppRolesService } from '../../../service/approle/app-role.service';
import { AppRole } from '../../../model/approle/app-role.model';
import { SessionService } from '../../../service/session.service';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-role-detail',
  templateUrl: './app-role-detail.component.html',
  styleUrls: ['./app-role-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, InfoSectionComponent, TranslocoModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class AppRoleDetailComponent {
  private appRolesService = inject(AppRolesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);

  lang = computed(() => this.sessionService.language());

  role = signal<AppRole | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  // Permission helpers for template bindings
  protected permissionService = inject(PermissionService);
  canWrite = computed(() => this.permissionService.hasWritePermission('AppRole'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('AppRole'));

  infoSections = computed<InfoSection[]>(() => {
    const o = this.role();
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
      const idParam = params['id'];
      const id = parseInt(idParam, 10);
      this.loadRole(id);
    });
  }

  private loadRole(id: number): void {
    this.appRolesService.getAppRole(id).subscribe({
      next: (role) => this.role.set(role),
      error: () => {
        this.error.set('App Role not found');
      }
    });
  }

  deleteRole(): void {
    const role = this.role();
    if (!role) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.appRolesService.deleteAppRole(role.id).subscribe({
        next: () => {
          this.router.navigate(['/' + this.lang(), 'app-roles']);
        },
        error: () => {
          this.error.set(this.transloco.translate('common.errors.appRole.deleteError'));
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.role() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) {
      return;
    }
    this.showEditKeyHint.set(true);
    const role = this.role();
    if (role) {
      this.router.navigate(['/' + this.lang(), 'app-roles', role.id, 'edit']);
    }
  }
}
