import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { SessionService } from '../../service/session.service';
import { PermissionService } from '../../service/permission.service';
import { ServiceInitializationService, DataFilesPreview } from '../../service/service-initialization.service';

@Component({
  selector: 'app-service-initialization',
  templateUrl: './service-initialization.component.html',
  styleUrls: ['./service-initialization.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoModule],
  providers: [ServiceInitializationService]
})
export class ServiceInitializationComponent {
  private sessionService = inject(SessionService);
  private serviceInitializationService = inject(ServiceInitializationService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  loadEssential = signal(false);
  loadSample = signal(false);

  preview = signal<DataFilesPreview | null>(null);
  loading = signal(false);
  message = signal<string | null>(null);
  messageType = signal<'success' | 'error' | null>(null);

  ngOnInit() {
    this.loadPreview();
  }

  loadPreview() {
    this.loading.set(true);
    this.serviceInitializationService.getDataFilesPreview().subscribe({
      next: (data) => {
        this.preview.set(data);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading preview:', error);
        this.loading.set(false);
        this.showMessage('Error loading file preview', 'error');
      }
    });
  }

  loadData() {
    const loadEssentialChecked = this.loadEssential();
    const loadSampleChecked = this.loadSample();

    if (!loadEssentialChecked && !loadSampleChecked) {
      this.showMessage('Please select at least one option', 'error');
      return;
    }

    this.loading.set(true);
    this.message.set(null);

    this.serviceInitializationService.loadData(loadEssentialChecked, loadSampleChecked).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.showMessage(response.message, response.status === 'accepted' || response.status === 'success' ? 'success' : 'error');
        this.loadEssential.set(false);
        this.loadSample.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.showMessage('Error loading data: ' + (error.error?.message || error.message), 'error');
      }
    });
  }

  refreshPermissions() {
    this.permissionService.loadPermissions();
    this.showMessage('Permissions refreshed successfully', 'success');
  }

  showMessage(msg: string, type: 'success' | 'error') {
    this.message.set(msg);
    this.messageType.set(type);
    setTimeout(() => {
      this.message.set(null);
      this.messageType.set(null);
    }, 5000);
  }
}
