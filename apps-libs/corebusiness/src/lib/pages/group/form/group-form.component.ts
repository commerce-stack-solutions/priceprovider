import { Component, signal, inject, OnInit, Input, Output, EventEmitter, computed } from '@angular/core';

import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { GroupsService } from '../../../service/group/groups.service';
import { Group } from '../../../model/group/group.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { ReferenceListEditComponent, ReferenceOption, ReferenceDataSourceResult } from '../../../components/referencelist-edit/referencelist-edit.component';
import { ModalService } from '../../../service/modal.service';
import { GroupOrganizationCreateComponent } from '../../../components/group-organization-create/group-organization-create.component';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-group-form',
  templateUrl: './group-form.component.html',
  styleUrls: ['./group-form.component.scss'],
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, ReferenceListEditComponent, RouterModule, TranslocoModule, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class GroupFormComponent implements OnInit {
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
  @Output() saved = new EventEmitter<Group>();
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


  // Data source for groups reference-list-edit
  groupsDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const pageSize = 10; // Changed to 10 for testing "Load More" functionality
    // Build query for backend filtering - search in id OR name
    const query = searchTerm ? `path:*${searchTerm}* OR name:*${searchTerm}*` : undefined;

    return this.groupsService.getGroups(page, pageSize, undefined, undefined, undefined, query).pipe(
      map(response => {
        const filtered = response.items.filter(g => g.path).map(g => ({
          value: g.path as string,
          label: g.name ? `${g.path} - ${g.name}` : (g.path as string)
        }));

        // Check if there are more pages
        const paging = response.$info && typeof response.$info === 'object' && 'paging' in response.$info
          ? response.$info.paging as { page: number; 'page-size': number; 'total-items': number; 'total-pages': number } | undefined
          : undefined;
        const hasMore = !!(paging && (page + 1) < ((paging['total-pages'] ?? 0)));

        return {
          options: filtered,
          hasMore: hasMore
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
      if (!this.permissionService.hasWritePermission('Group')) {
        if (idParam) {
          this.router.navigate(['/' + this.lang(), 'groups', idParam]);
        } else {
          this.router.navigate(['/' + this.lang(), 'groups']);
        }
        return;
      }
      this.initFormAndLoadData(idParam);
    }
  }

  private initFormAndLoadData(id: string | null): void {
    this.initForm();

    if (this.isEditMode()) {
      this.loadGroup(id!);
    } else {
      // Load $meta for create mode to populate enum values
      this.groupsService.getMeta().subscribe({
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
      path: [{ value: '', disabled: this.isEditMode() }, Validators.required],
      name: ['', Validators.required],
      parentRefs: [[]],
      subRefs: [[]]
    });
  }

  loadGroup(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.groupsService.getGroup(id).subscribe({
      next: (group: Group) => {
        if (group.$meta) {
          this.meta.set(group.$meta);
        }
        const patchData: any = {
          path: group.path,
          name: group.name,
          parentRefs: group.parentRefs || [],
          subRefs: group.subRefs || []
        };

        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load group: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();

    if (formValue.name !== original.name) {
      patches.push({ op: 'replace', path: '/name', value: formValue.name });
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
          this.router.navigate(['/' + this.lang(), 'groups', this.id()]);
        }
        return;
      }

      this.groupsService.patchGroup(this.id()!, patches).subscribe({
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
              this.router.navigate(['/' + this.lang(), 'groups', this.id()]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save group');
          }
          this.saving.set(false);
        }
      });
    } else {
      const group: Group = {
        path: formValue.path,
        name: formValue.name,
        parentRefs: formValue.parentRefs || [],
        subRefs: formValue.subRefs || []
      };

      this.groupsService.createGroup(group).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            if (this.isModalMode()) {
              this.saved.emit(response);
            } else {
              this.router.navigate(['/' + this.lang(), 'groups', response.id]);
            }
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save group');
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
      this.router.navigate(['/' + this.lang(), 'groups', this.id()]);
    } else {
      // Navigate to list
      this.router.navigate(['/' + this.lang(), 'groups']);
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

  async onCreateNewParentGroup(searchTerm: string): Promise<void> {
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

  async onCreateNewSubGroup(searchTerm: string): Promise<void> {
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
