import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AppPermissionsService } from '../../../service/approle/app-permission.service';
import { AppPermission } from '../../../model/approle/app-permission.model';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { MetaInfo } from '../../../model/meta-info.model';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

interface JsonPatchOperation {
  op: 'add' | 'remove' | 'replace' | 'move' | 'copy' | 'test';
  path: string;
  value?: any;
  from?: string;
}

@Component({
  selector: 'app-app-permission-form',
  templateUrl: './app-permission-form.component.html',
  styleUrls: ['./app-permission-form.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class AppPermissionFormComponent implements OnInit {
  private appPermissionsService = inject(AppPermissionsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);
  private transloco = inject(TranslocoService);

  lang = computed(() => this.sessionService.language());

  @Input() config?: { initialValue?: string; modalMode?: boolean };
  @Output() saved = new EventEmitter<AppPermission>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = signal(false);
  isModalMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  id = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);

  ngOnInit(): void {
    if (this.config?.modalMode) {
      this.isModalMode.set(true);
      this.isEditMode.set(false);
      this.initForm();
      this.loading.set(false);
      if (this.config?.initialValue) {
        this.form.patchValue({ name: this.config.initialValue });
      }
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    this.isEditMode.set(!!idParam);
    this.id.set(idParam);

    if (!this.permissionService.hasWritePermission('AppPermission')) {
      if (idParam) {
        this.router.navigate(['/' + this.lang(), 'app-permissions', idParam]);
      } else {
        this.router.navigate(['/' + this.lang(), 'app-permissions']);
      }
      return;
    }

    this.initFormAndLoadData(idParam);
  }

  private initFormAndLoadData(id: string | null): void {
    this.initForm();
    if (this.isEditMode()) {
      this.loadPermission(id!);
    } else {
      this.appPermissionsService.getMeta().subscribe({
        next: (metaInfo) => {
          this.meta.set(metaInfo);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      name: [{ value: '', disabled: this.isEditMode() }, [Validators.required]],
      description: ['']
    });
  }

  loadPermission(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.appPermissionsService.getAppPermission(id).subscribe({
      next: (permission: AppPermission) => {
        const patchData: any = {
          name: permission.name,
          description: permission.description || ''
        };
        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        if (permission.$meta) {
          this.meta.set(permission.$meta);
        }
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set(this.transloco.translate('common.errors.processing', { entity: 'AppPermission', details: err?.message || 'Unknown error' }));
        this.loading.set(false);
      }
    });
  }

  buildPatchOperations(): JsonPatchOperation[] {
    const patches: JsonPatchOperation[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();

    if (formValue.description !== original.description) {
      patches.push({ op: 'replace', path: '/description', value: formValue.description });
    }

    return patches;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const formValue = this.form.getRawValue();

    if (this.isEditMode()) {
      const patches = this.buildPatchOperations();

      if (patches.length === 0) {
        this.saving.set(false);
        if (this.isModalMode()) {
          this.cancelled.emit();
        } else {
          this.router.navigate(['/' + this.lang(), 'app-permissions', this.id()]);
        }
        return;
      }

      this.appPermissionsService.patchAppPermission(this.id()!, patches).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'app-permissions', this.id()]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || this.transloco.translate('common.errors.saveError'));
          }
          this.saving.set(false);
        }
      });
    } else {
      const permission: AppPermission = {
        name: formValue.name,
        description: formValue.description
      };

      this.appPermissionsService.createAppPermission(permission).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'app-permissions', response.name]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || this.transloco.translate('common.errors.saveError'));
          }
          this.saving.set(false);
        }
      });
    }
  }

  handleValidationErrors(messages: any[]): void {
    const fieldErrorsMap = new Map<string, string[]>();
    const errorMessages: string[] = [];

    messages.forEach((msg: Message) => {
      if (msg.type === 'ERROR') {
        const translatedMessage = this.messageTranslationService.translateMessage(msg);
        errorMessages.push(translatedMessage);

        if (msg.fields && msg.fields.length > 0) {
          msg.fields.forEach((field: string) => {
            if (!fieldErrorsMap.has(field)) {
              fieldErrorsMap.set(field, []);
            }
            fieldErrorsMap.get(field)!.push(translatedMessage);
          });
        }
      }
    });

    this.fieldErrors.set(fieldErrorsMap);
    this.error.set(errorMessages.join('; '));
  }

  hasFieldError(fieldName: string): boolean {
    return this.fieldErrors().has(fieldName);
  }

  getFieldErrors(fieldName: string): string[] {
    return this.fieldErrors().get(fieldName) || [];
  }

  cancel(): void {
    if (this.isModalMode()) {
      this.cancelled.emit();
    } else if (this.isEditMode()) {
      this.router.navigate(['/' + this.lang(), 'app-permissions', this.id()]);
    } else {
      this.router.navigate(['/' + this.lang(), 'app-permissions']);
    }
  }

  handleSaveKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (this.isModalMode() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) {
      return;
    }
    event.preventDefault();
    this.showSaveKeyHint.set(true);
    this.onSubmit();
    setTimeout(() => this.showSaveKeyHint.set(false), 500);
  }
}
