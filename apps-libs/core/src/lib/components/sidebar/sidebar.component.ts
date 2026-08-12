import { ChangeDetectionStrategy, Component, input, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { LabelService } from '../../service/label.service';
import { PermissionService } from '../../service/permission.service';
import { getItemIcon, getItemLabel, getLink, getSectionLabel, getVisibleMenuSections } from '../../service/menu-registry';
import { RegistryService } from '../../service/registry.service';
import { SessionService } from '../../service/session.service';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
  standalone: true,
  imports: [RouterLink, RouterLinkActive, TranslocoModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {
  visible = input<boolean>(true);
  private labelService = inject(LabelService);
  private permissionService = inject(PermissionService);
  private registry = inject(RegistryService);
  private sessionService = inject(SessionService);

  lang = computed(() => this.sessionService.language());

  sections = computed(() => getVisibleMenuSections(this.registry, this.permissionService));
  protected readonly getSectionLabel = getSectionLabel;
  protected readonly getItemIcon = getItemIcon;

  protected getItemLabel = (item: any) => getItemLabel(this.labelService, item);
  protected getLink = (item: any) => getLink(this.lang(), item);
}