import { Component, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { GroupsService } from '../../../service/group/groups.service';
import { Group } from '../../../model/group/group.model';
import { SessionService } from '../../../service/session.service';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-group-detail',
  templateUrl: './group-detail.component.html',
  styleUrls: ['./group-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, RouterModule, InfoSectionComponent, TranslocoModule],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class GroupDetailComponent {
  private groupsService = inject(GroupsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sessionService = inject(SessionService);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  group = signal<Group | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  canWrite = computed(() => this.permissionService.hasWritePermission('Group'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Group'));

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.group();
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
    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt' && k !== 'lastModifiedAt' && k !== 'parentRefIds' && k !== 'subRefIds');
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
      const id = params['id'];
      this.loadGroup(id);
    });
  }

  private loadGroup(id: string): void {
    this.groupsService.getGroup(id).subscribe({
      next: (group) => this.group.set(group),
      error: (error) => {
        this.error.set('Group not found');
        console.error('Error loading group:', error);
      }
    });
  }

  deleteGroup(): void {
    if (!this.canDelete()) return;

    const group = this.group();
    if (!group) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.groupsService.deleteGroup(group.id!).subscribe({
        next: () => {
          this.router.navigate(['/groups']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.group.deleteError'));
          console.error('Error deleting group:', error);
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    // Ensure keyboard event and guard against input/textarea targets
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (
      !this.group() ||
      (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))
    ) {
      return;
    }

    // Show visual feedback
    this.showEditKeyHint.set(true);

    // Navigate to edit page
    const group = this.group();
    if (group && this.canWrite()) {
      this.router.navigate(['/' + this.lang(), 'groups', group.id, 'edit']);
    }
  }
}
