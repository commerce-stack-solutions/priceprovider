import { Component, signal, inject, OnInit, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AppRolesService } from '../../../service/approle/app-role.service';
import { AppPermissionsService } from '../../../service/approle/app-permission.service';
import { AppRole } from '../../../model/approle/app-role.model';
import { AppPermission } from '../../../model/approle/app-permission.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { ReferenceListEditComponent, ReferenceOption, ReferenceDataSourceResult } from '../../../components/referencelist-edit/referencelist-edit.component';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-role-form',
  templateUrl: './app-role-form.component.html',
  styleUrls: ['./app-role-form.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ReferenceListEditComponent, RouterModule, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class AppRoleFormComponent implements OnInit {
  private appRolesService = inject(AppRolesService);
  private appPermissionsService = inject(AppPermissionsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  @Input() config?: { initialValue?: string; modalMode?: boolean };
  @Output() saved = new EventEmitter<AppRole>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  isEditMode = signal(false);
  isModalMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  id = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);

  // Data source for app permissions reference-list-edit
  permissionsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const pageSize = 10;
    const query = searchTerm ? `id:*${searchTerm}* OR description:*${searchTerm}*` : undefined;

    return this.appPermissionsService.getAppPermissions(page, pageSize, undefined, undefined, undefined, query).pipe(
      map(response => {
        const filtered = response.items.map((p: AppPermission) => ({
          value: p.id,
          label: p.description ? `${p.id} - ${p.description}` : p.id
        }));

        const paging = response.$info && typeof response.$info === 'object' && 'paging' in response.$info
          ? response.$info.paging as { page: number; 'page-size': number; 'total-items': number; 'total-pages': number } | undefined
          : undefined;
        const hasMore = !!(paging && (page + 1) < ((paging['total-pages'] ?? 0)));

        return { options: filtered, hasMore };
      })
    );
  };

  ngOnInit(): void {
    if (this.config?.modalMode) {
      this.isModalMode.set(true);
      this.isEditMode.set(false);
      this.initForm();
      this.loading.set(false);
      if (this.config?.initialValue) {
        this.form.patchValue({ id: this.config.initialValue });
      }
    } else {
      const idParam = this.route.snapshot.paramMap.get('id');
      this.isEditMode.set(!!idParam);
      this.id.set(idParam);
      if (!this.permissionService.hasWritePermission('AppRole')) {
        if (idParam) {
          this.router.navigate(['/' + this.lang(), 'app-roles', idParam]);
        } else {
          this.router.navigate(['/' + this.lang(), 'app-roles']);
        }
        return;
      }
      this.initFormAndLoadData(idParam);
    }
  }

  private initFormAndLoadData(id: string | null): void {
    this.initForm();
    if (this.isEditMode()) {
      this.loadRole(id!);
    } else {
      this.appRolesService.getMeta().subscribe({
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
      id: [{ value: '', disabled: this.isEditMode() }, Validators.required],
      description: [''],
      permissionRefs: [[]]
    });
  }

  loadRole(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.appRolesService.getAppRole(id).subscribe({
      next: (role: AppRole) => {
        if (role.$meta) {
          this.meta.set(role.$meta);
        }
        const patchData: any = {
          id: role.id,
          description: role.description || '',
          permissionRefs: role.permissionRefs || []
        };
        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load app role: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();

    if (formValue.description !== original.description) {
      patches.push({ op: 'replace', path: '/description', value: formValue.description });
    }

    const currentPermissions = formValue.permissionRefs || [];
    const originalPermissions = original.permissionRefs || [];
    if (JSON.stringify([...currentPermissions].sort()) !== JSON.stringify([...originalPermissions].sort())) {
      patches.push({ op: 'replace', path: '/permissionRefs', value: currentPermissions });
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
          this.router.navigate(['/' + this.lang(), 'app-roles', this.id()]);
        }
        return;
      }

      this.appRolesService.patchAppRole(this.id()!, patches).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'app-roles', this.id()]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save app role');
          }
          this.saving.set(false);
        }
      });
    } else {
      const role: AppRole = {
        id: formValue.id,
        description: formValue.description,
        permissionRefs: formValue.permissionRefs || []
      };

      this.appRolesService.createAppRole(role).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'app-roles', response.id]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save app role');
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
      this.router.navigate(['/' + this.lang(), 'app-roles', this.id()]);
    } else {
      this.router.navigate(['/' + this.lang(), 'app-roles']);
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
