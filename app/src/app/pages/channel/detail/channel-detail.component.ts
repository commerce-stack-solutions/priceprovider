import { Component, signal, inject, computed } from '@angular/core';

import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ChannelsService } from '../../../service/channel/channels.service';
import { Channel } from '../../../model/channel/channel.model';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { LabelService } from '../../../service/label.service';
import { DateTimeService } from '../../../service/datetime.service';
import { SessionService } from '../../../service/session.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-channel-detail',
  templateUrl: './channel-detail.component.html',
  styleUrls: ['./channel-detail.component.scss'],
  standalone: true,
  imports: [RouterModule, InfoSectionComponent, TranslocoModule],
  host: { '(document:keydown.e)': 'handleEditKeyPress($event)' }
})
export class ChannelDetailComponent {
  private channelsService = inject(ChannelsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);

  lang = computed(() => this.sessionService.language());

  channel = signal<Channel | null>(null);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  // Permission helpers
  protected permissionService = inject(PermissionService);
  canWrite = computed(() => this.permissionService.hasWritePermission('Channel'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Channel'));

  infoSections = computed<InfoSection[]>(() => {
    const o = this.channel();
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
      const id = params['id'];
      this.loadChannel(id);
    });
  }

  private loadChannel(id: string): void {
    this.channelsService.getChannel(id).subscribe({
      next: (channel) => this.channel.set(channel),
      error: (error) => { this.error.set('Channel not found'); console.error('Error loading channel:', error); }
    });
  }

  deleteChannel(): void {
    if (!this.canDelete()) return;
    const channel = this.channel();
    if (!channel) return;
    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.channelsService.deleteChannel(channel.id).subscribe({
        next: () => { this.router.navigate(['/' + this.lang(), 'channels']); },
        error: (error) => { this.error.set(this.transloco.translate('common.errors.channel.deleteError')); console.error('Error deleting channel:', error); }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.channel() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) return;
    this.showEditKeyHint.set(true);
    const channel = this.channel();
    if (channel) this.router.navigate(['/' + this.lang(), 'channels', channel.id, 'edit']);
  }

  getPriceRepresentationModeDescription(mode: string | null | undefined): string {
    if (!mode) return '';
    const descriptions: { [key: string]: string } = {
      'NET_ONLY': 'Publish only prices that are already defined as net. Prices defined as gross are excluded.',
      'GROSS_ONLY': 'Publish only prices that are already defined as gross. Prices defined as net are excluded.',
      'FORCE_NET': 'Publish all prices as net. Any gross prices are converted to net before publishing.',
      'FORCE_GROSS': 'Publish all prices as gross. Any net prices are converted to gross before publishing.'
    };
    return descriptions[mode] || mode;
  }
}
