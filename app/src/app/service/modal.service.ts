import { Injectable, Type, ComponentRef, ViewContainerRef, EnvironmentInjector, createComponent, signal } from '@angular/core';

export interface ModalConfig {
  title: string;
  size?: 'sm' | 'lg' | 'xl';
  data?: any;
}

export interface ModalResult {
  success: boolean;
  data?: any;
}

@Injectable({
  providedIn: 'root'
})
export class ModalService {
  private modalContainerRef?: ViewContainerRef;
  private environmentInjector?: EnvironmentInjector;
  private currentModal = signal<any>(null);

  setContainer(container: ViewContainerRef, injector: EnvironmentInjector): void {
    this.modalContainerRef = container;
    this.environmentInjector = injector;
  }

  open<T extends object>(component: Type<T>, config: ModalConfig): Promise<ModalResult> {
    return new Promise((resolve) => {
      if (!this.modalContainerRef || !this.environmentInjector) {
        console.error('Modal container not initialized');
        resolve({ success: false });
        return;
      }

      // Create modal backdrop and dialog
      const modalBackdrop = document.createElement('div');
      modalBackdrop.className = 'modal-backdrop fade show';
      document.body.appendChild(modalBackdrop);

      const modalElement = document.createElement('div');
      modalElement.className = 'modal fade show';
      modalElement.style.display = 'block';
      modalElement.setAttribute('tabindex', '-1');
      modalElement.setAttribute('role', 'dialog');
      
      const sizeClass = config.size ? `modal-${config.size}` : '';
      modalElement.innerHTML = `
        <div class="modal-dialog ${sizeClass}" role="document">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${config.title}</h5>
              <button type="button" class="btn-close modal-close-btn" aria-label="Close"></button>
            </div>
            <div class="modal-body">
              <div id="modal-component-container"></div>
            </div>
          </div>
        </div>
      `;
      
      document.body.appendChild(modalElement);
      document.body.classList.add('modal-open');

      // Get container for component
      const componentContainer = modalElement.querySelector('#modal-component-container');
      if (!componentContainer) {
        this.cleanup(modalElement, modalBackdrop);
        resolve({ success: false });
        return;
      }

      // Create component
      const componentRef = createComponent(component, {
        environmentInjector: this.environmentInjector,
        hostElement: componentContainer
      });

      // Pass config data to component if it has a config property
      if ('config' in componentRef.instance) {
        (componentRef.instance as any).config = config.data;
      }

      // Trigger change detection to ensure ngOnInit runs
      componentRef.changeDetectorRef.detectChanges();

      // Listen for save/cancel events
      if ('saved' in componentRef.instance) {
        (componentRef.instance as any).saved.subscribe((data: any) => {
          this.cleanup(modalElement, modalBackdrop);
          componentRef.destroy();
          resolve({ success: true, data });
        });
      }

      if ('cancelled' in componentRef.instance) {
        (componentRef.instance as any).cancelled.subscribe(() => {
          this.cleanup(modalElement, modalBackdrop);
          componentRef.destroy();
          resolve({ success: false });
        });
      }

      // Handle close button click
      const closeBtn = modalElement.querySelector('.modal-close-btn');
      closeBtn?.addEventListener('click', () => {
        this.cleanup(modalElement, modalBackdrop);
        componentRef.destroy();
        resolve({ success: false });
      });

      // Handle backdrop click
      modalElement.addEventListener('click', (e) => {
        if (e.target === modalElement) {
          this.cleanup(modalElement, modalBackdrop);
          componentRef.destroy();
          resolve({ success: false });
        }
      });

      this.currentModal.set({ modalElement, modalBackdrop, componentRef });
    });
  }

  private cleanup(modalElement: HTMLElement, modalBackdrop: HTMLElement): void {
    modalElement.remove();
    modalBackdrop.remove();
    document.body.classList.remove('modal-open');
    this.currentModal.set(null);
  }

  close(): void {
    const modal = this.currentModal();
    if (modal) {
      this.cleanup(modal.modalElement, modal.modalBackdrop);
      modal.componentRef.destroy();
    }
  }
}
