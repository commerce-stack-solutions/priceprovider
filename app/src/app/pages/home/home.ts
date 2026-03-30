import { Component, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { SessionService } from '../../service/session.service';
import { PermissionService } from '../../service/permission.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrls: ['./home.scss'],
  standalone: true,
  imports: [RouterLink, TranslocoModule]
})
export class HomeComponent {
  private sessionService = inject(SessionService);
  protected permissionService = inject(PermissionService);
  lang = computed(() => this.sessionService.language());
}
