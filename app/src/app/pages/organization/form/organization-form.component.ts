import { Component, signal, inject, OnInit, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrganizationsService } from '../../../service/organization/organizations.service';
import { GroupsService } from '../../../service/group/groups.service';
import { Organization, OrganizationType } from '../../../model/organization/organization.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { EnumSelectorComponent } from '../../../components/enum-selector/enum-selector.component';
import { ReferenceListEditComponent, ReferenceOption, ReferenceDataSourceResult } from '../../../components/referencelist-edit/referencelist-edit.component';
import { ModalService } from '../../../service/modal.service';
import { GroupOrganizationCreateComponent } from '../../../components/group-organization-create/group-organization-create.component';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-organization-form',
  templateUrl: './organization-form.component.html',
  styleUrls: ['./organization-form.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, EnumSelectorComponent, ReferenceListEditComponent, RouterModule, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class OrganizationFormComponent implements OnInit {
  private organizationsService = inject(OrganizationsService);
  private groupsService = inject(GroupsService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private modalService = inject(ModalService);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  @Input() config?: { initialValue?: string; modalMode?: boolean };
  @Output() saved = new EventEmitter<Organization>();
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

  // Enum values - fallback when $meta is not available
  organizationTypeValues: string[] = Object.values(OrganizationType);

  // Data source for groups (includes organizations)
  groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const pageSize = 10;
    // Build query for backend filtering - search in id OR name
    const query = searchTerm ? `path:*${searchTerm}* OR name:*${searchTerm}*` : undefined;

    return this.groupsService.getGroups(page, pageSize, undefined, undefined, undefined, query).pipe(
      catchError(() => of({ items: [], $info: {} })),
      map(response => {
        const options = (response.items || []).filter(g => g.path).map(g => ({
          value: g.path as string,
          label: g.name ? `${g.path} - ${g.name}` : (g.path as string)
        }));

        // Check if there are more items
        const paging = (response.$info && typeof response.$info === 'object' && 'paging' in response.$info)
          ? (response.$info.paging as { page: number; 'page-size': number; 'total-items': number; 'total-pages': number } | undefined)
          : undefined;
        const hasMore = !!(paging && (page + 1) < ((paging['total-pages'] ?? 0)));

        return {
          options,
          hasMore
        };
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
        this.form.patchValue({ path: this.config.initialValue });
      }
    } else {
      const idParam = this.route.snapshot.paramMap.get('id');
      this.isEditMode.set(!!idParam);
      this.id.set(idParam);
      if (!this.permissionService.hasWritePermission('Organization')) {
        if (idParam) {
          this.router.navigate(['/' + this.lang(), 'organizations', idParam]);
        } else {
          this.router.navigate(['/' + this.lang(), 'organizations']);
        }
        return;
      }
      this.initFormAndLoadData(idParam);
    }
  }

  private initFormAndLoadData(id: string | null): void {
    this.initForm();

    if (this.isEditMode()) {
      this.loadOrganization(id!);
    } else {
      // Load $meta for create mode to populate enum values
      this.organizationsService.getMeta().subscribe({
        next: (metaInfo) => {
          this.meta.set(metaInfo);
          if (metaInfo.enumValues?.['organizationType']) {
            this.organizationTypeValues = metaInfo.enumValues['organizationType'];
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      path: [{ value: '', disabled: this.isEditMode() }, Validators.required],
      name: ['', Validators.required],
      organizationType: ['', Validators.required],
      parentRefs: [[]],
      subRefs: [[]]
    });
  }

  loadOrganization(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.organizationsService.getOrganization(id).subscribe({
      next: (org: Organization) => {
        if (org.$meta) {
          this.meta.set(org.$meta);
          if (org.$meta.enumValues?.['organizationType']) {
            this.organizationTypeValues = org.$meta.enumValues['organizationType'];
          }
        }
        const patchData: any = {
          path: org.path,
          name: org.name,
          organizationType: org.organizationType,
          parentRefs: org.parentRefs || [],
          subRefs: org.subRefs || []
        };

        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load organization: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) {
      return m.mandatoryFields.includes(fieldName);
    }
    // Fallback: use form validator presence
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();

    if (formValue.name !== original.name) {
      patches.push({ op: 'replace', path: '/name', value: formValue.name });
    }

    if (formValue.organizationType !== original.organizationType) {
      patches.push({ op: 'replace', path: '/organizationType', value: formValue.organizationType });
    }

    // Handle parentRefs
    const currentParents = formValue.parentRefs || [];
    const originalParents = original.parentRefs || [];
    if (JSON.stringify(currentParents.sort()) !== JSON.stringify(originalParents.sort())) {
      patches.push({ op: 'replace', path: '/parentRefs', value: currentParents });
    }

    // Handle subRefs
    const currentSubs = formValue.subRefs || [];
    const originalSubs = original.subRefs || [];
    if (JSON.stringify(currentSubs.sort()) !== JSON.stringify(originalSubs.sort())) {
      patches.push({ op: 'replace', path: '/subRefs', value: currentSubs });
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
        // No changes, navigate to detail view
        this.saving.set(false);
        if (this.isModalMode()) {
          this.cancelled.emit();
        } else {
          this.router.navigate(['/' + this.lang(), 'organizations', this.id()]);
        }
        return;
      }

      this.organizationsService.patchOrganization(this.id()!, patches).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            if (this.isModalMode()) {
              this.saving.set(false);
              this.saved.emit(response);
            } else {
              // Navigate to detail view after successful save
              this.saving.set(false);
              this.router.navigate(['/' + this.lang(), 'organizations', this.id()]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save organization');
          }
          this.saving.set(false);
        }
      });
    } else {
      const org: Organization = {
        path: formValue.path,
        name: formValue.name,
        organizationType: formValue.organizationType,
        parentRefs: formValue.parentRefs || [],
        subRefs: formValue.subRefs || []
      };

      this.organizationsService.createOrganization(org).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'organizations', response.id]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save organization');
          }
          this.saving.set(false);
        }
      });
    }
  }

  handleValidationErrors(messages: any[]): void {
    console.log('handleValidationErrors called with:', messages);
    const fieldErrorsMap = new Map<string, string[]>();
    const errorMessages: string[] = [];

    messages.forEach((msg: Message) => {
      if (msg.type === 'ERROR') {
        // Translate the message using the message translation service
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

    console.log('fieldErrorsMap:', fieldErrorsMap);
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
      // Navigate to detail view
      this.router.navigate(['/' + this.lang(), 'organizations', this.id()]);
    } else {
      // Navigate to list
      this.router.navigate(['/' + this.lang(), 'organizations']);
    }
  }

  handleSaveKeyPress(event: Event): void {
    // Ensure keyboard event and guard against input/textarea targets
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (
      this.isModalMode() ||
      (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))
    ) {
      return;
    }

    // Prevent default 's' key behavior (e.g., browser save)
    event.preventDefault();

    // Show visual feedback
    this.showSaveKeyHint.set(true);

    // Submit the form
    this.onSubmit();

    // Hide hint after a short delay
    setTimeout(() => {
      this.showSaveKeyHint.set(false);
    }, 500);
  }

  async onCreateNewParentOrganization(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(GroupOrganizationCreateComponent, {
      title: 'Create New Group or Organization',
      size: 'lg',
      data: { initialValue: searchTerm }
    });

    if (result.success && result.data) {
      const currentParents = this.form.get('parentRefs')?.value || [];
      if (!currentParents.includes(result.data.path)) {
        this.form.get('parentRefs')?.setValue([...currentParents, result.data.path]);
      }
    }
  }

  async onCreateNewSubOrganization(searchTerm: string): Promise<void> {
    const result = await this.modalService.open(GroupOrganizationCreateComponent, {
      title: 'Create New Group or Organization',
      size: 'lg',
      data: { initialValue: searchTerm }
    });

    if (result.success && result.data) {
      const currentSubs = this.form.get('subRefs')?.value || [];
      if (!currentSubs.includes(result.data.path)) {
        this.form.get('subRefs')?.setValue([...currentSubs, result.data.path]);
      }
    }
  }
}
