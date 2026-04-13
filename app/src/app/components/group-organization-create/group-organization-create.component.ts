import { Component, signal, inject, OnInit, Input, Output, EventEmitter, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { GroupsService } from '../../service/group/groups.service';
import { OrganizationsService } from '../../service/organization/organizations.service';
import { Group } from '../../model/group/group.model';
import { Organization, OrganizationType } from '../../model/organization/organization.model';

@Component({
  selector: 'app-group-organization-create',
  templateUrl: './group-organization-create.component.html',
  styleUrls: ['./group-organization-create.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TranslocoModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupOrganizationCreateComponent implements OnInit {
  private groupsService = inject(GroupsService);
  private organizationsService = inject(OrganizationsService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  @Input() config?: { initialValue?: string };
  @Output() saved = new EventEmitter<Group | Organization>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = signal(false);
  error = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());

  // Type selection
  entityType = signal<'group' | 'organization'>('group');

  // Enum values
  organizationTypeValues: OrganizationType[] = [
    OrganizationType.HOST_ORGANIZATION,
    OrganizationType.COMPANY,
    OrganizationType.BUSINESS_UNIT,
    OrganizationType.PUBLIC_INSTITUTION,
    OrganizationType.DEPARTMENT,
    OrganizationType.NON_PROFIT_ORGANIZATION
  ];

  ngOnInit(): void {
    this.initForm();
    if (this.config?.initialValue) {
      this.form.patchValue({ path: this.config.initialValue });
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      path: ['', Validators.required],
      name: ['', Validators.required],
      organizationType: ['']
    });
  }

  onEntityTypeChange(type: 'group' | 'organization'): void {
    this.entityType.set(type);
    
    // Update validators based on type
    const orgTypeControl = this.form.get('organizationType');
    if (type === 'organization') {
      orgTypeControl?.setValidators([Validators.required]);
    } else {
      orgTypeControl?.clearValidators();
      orgTypeControl?.setValue('');
    }
    orgTypeControl?.updateValueAndValidity();
    // Trigger change detection for zoneless mode
    this.cdr.detectChanges();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      
      // Show error message for missing required fields
      const missingFields = [];
      if (this.form.get('path')?.invalid) missingFields.push('Path');
      if (this.form.get('name')?.invalid) missingFields.push('Name');
      if (this.entityType() === 'organization' && this.form.get('organizationType')?.invalid) {
        missingFields.push('Organization Type');
      }
      
      if (missingFields.length > 0) {
        this.error.set(`Please fill in all required fields: ${missingFields.join(', ')}`);
      }
      
      // Trigger change detection to show errors
      this.cdr.detectChanges();
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const formValue = this.form.value;

    if (this.entityType() === 'group') {
      const group: Group = {
        path: formValue.path,
        name: formValue.name,
        parentRefs: [],
        subRefs: []
      };

      this.groupsService.createGroup(group).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.stopSaving();
          } else {
            this.stopSaving();
            this.saved.emit(response);
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save group');
          }
          this.stopSaving();
        }
      });
    } else {
      const organization: Organization = {
        path: formValue.path,
        name: formValue.name,
        organizationType: formValue.organizationType,
        parentRefs: [],
        subRefs: []
      };

      this.organizationsService.createOrganization(organization).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.stopSaving();
          } else {
            this.stopSaving();
            this.saved.emit(response);
          }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save organization');
          }
          this.stopSaving();
        }
      });
    }
  }

  handleValidationErrors(messages: any[]): void {
    const fieldErrorsMap = new Map<string, string[]>();
    const errorMessages: string[] = [];

    messages.forEach((msg: any) => {
      if (msg.type === 'ERROR') {
        errorMessages.push(msg.message);
        
        if (msg.fields && msg.fields.length > 0) {
          msg.fields.forEach((field: string) => {
            if (!fieldErrorsMap.has(field)) {
              fieldErrorsMap.set(field, []);
            }
            fieldErrorsMap.get(field)!.push(msg.message);
          });
        }
      }
    });

    this.fieldErrors.set(fieldErrorsMap);
    this.error.set(errorMessages.join('; '));
    this.cdr.detectChanges();
  }

  hasFieldError(fieldName: string): boolean {
    return this.fieldErrors().has(fieldName);
  }

  getFieldErrors(fieldName: string): string[] {
    return this.fieldErrors().get(fieldName) || [];
  }

  cancel(): void {
    this.cancelled.emit();
  }

  private stopSaving(): void {
    this.saving.set(false);
    this.cdr.detectChanges();
  }

  formatEnumLabel(value: string): string {
    return value.split('_').map(word => 
      word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
  }
}
