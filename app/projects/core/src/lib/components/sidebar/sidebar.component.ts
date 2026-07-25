import { ChangeDetectionStrategy, Component, input, inject, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { SessionService } from '../../service/session.service';
import { PermissionService } from '../../service/permission.service';

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
  private sessionService = inject(SessionService);
  protected permissionService = inject(PermissionService);
  
  // Computed value for current language to use in templates
  lang = computed(() => this.sessionService.language());
}