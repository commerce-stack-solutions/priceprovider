import { Component, inject, OnInit, signal, computed, ChangeDetectorRef } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ReferenceDataSource, ReferenceDataSourceResult, ReferenceEditComponent } from '../../components/reference-edit/reference-edit.component';
import { ReferenceListEditComponent } from '../../components/referencelist-edit/referencelist-edit.component';
import { EnumSelectorComponent } from '../../components/enum-selector/enum-selector.component';
import { LocalizedStringfieldEditComponent } from '../../components/localized-stringfield-edit/localized-stringfield-edit.component';
import { SessionService } from '../../service/session.service';
import { PermissionService } from '../../service/permission.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { MessageTranslationService } from '../../service/message-translation.service';
import { Message } from '../../model/message.model';
import { MetaInfo, FieldMetadata } from '../../model/meta-info.model';
import { IsMandatoryPipe } from '../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-generic-form',
  templateUrl: './generic-form.component.html',
  styleUrls: ['./generic-form.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    ReferenceEditComponent,
    ReferenceListEditComponent,
    EnumSelectorComponent,
    LocalizedStringfieldEditComponent,
    TranslocoModule,
    IsMandatoryPipe,
    TitleCasePipe
  ],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class GenericFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private transloco = inject(TranslocoService);
  sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  form!: FormGroup;
  entityType = signal<string>('');
  isEditMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  id = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);

  // Map to hold localized field languages signal per field
  localizedLanguagesMap = new Map<string, any>();

  // Computed available language arrays
  mandatoryLanguages = computed(() => {
    return this.sessionService.availableLanguages()
      .filter(l => l.mandatory)
      .map(l => l.isoKey);
  });

  activeLanguages = computed(() => {
    return this.sessionService.availableLanguages()
      .filter(l => l.active && !l.mandatory)
      .map(l => l.isoKey);
  });

  inactiveLanguages = computed(() => {
    return this.sessionService.availableLanguages()
      .filter(l => !l.active)
      .map(l => l.isoKey);
  });

  // Track the original entity data
  originalEntity = signal<any>(null);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const type = params.get('entityType') || '';
      const id = params.get('id');
      this.entityType.set(type);
      this.isEditMode.set(!!id);
      this.id.set(id ?? null);

      if (this.sessionService.availableLanguages().length > 0) {
        this.loadMetaAndData();
      } else {
        setTimeout(() => this.loadMetaAndData(), 100);
      }
    });
  }

  private loadMetaAndData(): void {
    this.loading.set(true);
    this.error.set(null);
    this.fieldErrors.set(new Map());

    const metaUrl = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/$meta`;
    this.http.get<MetaInfo>(metaUrl).subscribe({
      next: (metaInfo) => {
        this.meta.set(metaInfo);
        this.initForm();

        if (this.isEditMode()) {
          this.loadEntity(this.id()!);
        } else {
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.error.set('Failed to load entity metadata: ' + (err.error?.message || err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  private initForm(): void {
    const formConfig: any = {};
    const metaInfo = this.meta();

    if (!metaInfo || !metaInfo.fields) {
      this.form = this.fb.group({});
      return;
    }

    metaInfo.fields.forEach(field => {
      const isMandatory = metaInfo.mandatoryFields?.includes(field.name) || false;

      if (field.type === 'LocalizedString') {
        // Initialize dynamic languages signal for this field
        const initialLangs = [...this.mandatoryLanguages(), ...this.activeLanguages()];
        const signalObj = signal<string[]>(initialLangs);
        this.localizedLanguagesMap.set(field.name, signalObj);

        // Add control for all languages
        this.sessionService.availableLanguages().forEach(l => {
          const isLangMandatory = this.mandatoryLanguages().includes(l.isoKey);
          formConfig[`${field.name}_${l.isoKey}`] = ['', isLangMandatory ? Validators.required : []];
        });
      } else {
        const validators = [];
        if (isMandatory) {
          validators.push(Validators.required);
        }
        if (field.type === 'Number') {
          validators.push(Validators.pattern(/^\-?\d*\.?\d*$/));
        }
        formConfig[field.name] = [{ value: '', disabled: field.readOnly && this.isEditMode() }, validators];
      }
    });

    this.form = this.fb.group(formConfig);
  }

  private loadEntity(id: string): void {
    const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/${encodeURIComponent(id)}`;
    const params = new HttpParams().set('$expand', '$includes,$info,$meta');

    this.http.get<any>(url, { params }).subscribe({
      next: (entity) => {
        this.originalEntity.set(entity);

        const patchData: any = {};
        const metaInfo = this.meta();

        if (metaInfo && metaInfo.fields) {
          metaInfo.fields.forEach(field => {
            if (field.type === 'LocalizedString') {
              const localizedMap = entity[field.name] || {};
              // Patch all language values
              this.sessionService.availableLanguages().forEach(l => {
                patchData[`${field.name}_${l.isoKey}`] = localizedMap[l.isoKey] || '';
              });

              // Adjust visible languages for this field
              const languagesToDisplay = [...this.mandatoryLanguages(), ...this.activeLanguages()];
              this.inactiveLanguages().forEach(l => {
                if (localizedMap[l] && localizedMap[l].trim() !== '' && !languagesToDisplay.includes(l)) {
                  languagesToDisplay.push(l);
                }
              });
              const sig = this.localizedLanguagesMap.get(field.name);
              if (sig) {
                sig.set(languagesToDisplay);
              }
            } else if (field.type === 'DateTime') {
              patchData[field.name] = entity[field.name] ? this.formatDateForInput(entity[field.name]) : '';
            } else {
              patchData[field.name] = entity[field.name] !== undefined && entity[field.name] !== null ? entity[field.name] : '';
            }
          });
        }

        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load entity data: ' + (err.error?.message || err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  private formatDateForInput(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toISOString().slice(0, 16); // format for datetime-local
  }

  getReferencedApiPlural(field: FieldMetadata): string {
    const nameToUse = field.referencedEntity || field.name;
    const lower = nameToUse.toLowerCase();
    if (lower.includes('unit')) return 'units';
    if (lower.includes('currency')) return 'currencies';
    if (lower.includes('taxclass')) return 'taxclasses';
    if (lower.includes('group')) return 'groups';
    if (lower.includes('channel')) return 'channels';
    if (lower.includes('language')) return 'languages';
    if (lower.includes('country')) return 'countries';
    if (lower.includes('organization')) return 'organizations';
    if (lower.includes('approle') || lower.includes('role')) return 'app-roles';
    if (lower.includes('permission')) return 'app-permissions';
    return lower.endsWith('s') ? lower : lower + 's';
  }

  getReferencedDisplayKey(apiPlural: string): string {
    switch (apiPlural) {
      case 'units': return 'symbol';
      case 'currencies': return 'currencyKey';
      case 'taxclasses': return 'taxClassId';
      case 'groups': return 'path';
      case 'channels': return 'id';
      case 'languages': return 'isoKey';
      case 'countries': return 'isoKey';
      case 'organizations': return 'path';
      case 'app-roles': return 'id';
      case 'app-permissions': return 'id';
      default: return 'id';
    }
  }

  getGenericDataSource(field: FieldMetadata): ReferenceDataSource {
    const apiPlural = this.getReferencedApiPlural(field);
    const displayKey = this.getReferencedDisplayKey(apiPlural);

    return (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('page-size', '30');
      if (searchTerm) {
        params = params.set('q', `${displayKey}:*${searchTerm}*`);
      }
      return this.http.get<any>(`${environment.apiBaseUrl}admin/api/${apiPlural}`, { params }).pipe(
        map(res => {
          const items = res.items || [];
          return {
            options: items.map((item: any) => ({
              value: item[displayKey] || item.id || item.currencyKey || item.symbol || item.path || item.isoKey || item.taxClassId,
              label: item[displayKey] || item.path || item.name || item.id || item.symbol || item.currencyKey || item.isoKey || item.taxClassId
            })),
            hasMore: res.$info?.paging ? res.$info.paging.page < res.$info.paging['total-pages'] - 1 : false
          };
        }),
        catchError(() => of({ options: [], hasMore: false }))
      );
    };
  }

  getFormControl(name: string): any {
    return this.form.get(name);
  }

  getLanguagesSignal(fieldName: string): any {
    return this.localizedLanguagesMap.get(fieldName);
  }

  addLocalizedLanguage(fieldName: string, lang: string): void {
    const sig = this.localizedLanguagesMap.get(fieldName);
    if (sig) {
      const current = sig();
      if (!current.includes(lang)) {
        sig.set([...current, lang]);
      }
    }
  }

  removeLocalizedLanguage(fieldName: string, lang: string): void {
    const sig = this.localizedLanguagesMap.get(fieldName);
    if (sig) {
      const current = sig();
      sig.set(current.filter((l: string) => l !== lang));
      const control = this.form.get(`${fieldName}_${lang}`);
      if (control) {
        control.setValue('');
      }
    }
  }

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();
    const metaInfo = this.meta();

    if (!metaInfo || !metaInfo.fields) return patches;

    metaInfo.fields.forEach(field => {
      if (field.readOnly) return;

      if (field.type === 'LocalizedString') {
        this.sessionService.availableLanguages().forEach(l => {
          const controlName = `${field.name}_${l.isoKey}`;
          const currentValue = formValue[controlName] || '';
          const originalValue = original[controlName] || '';

          if (currentValue !== originalValue) {
            if (currentValue === '' || currentValue === null) {
              if (originalValue !== '' && originalValue !== null && originalValue !== undefined) {
                patches.push({ op: 'remove', path: `/${field.name}/${l.isoKey}` });
              }
            } else {
              if (originalValue === '' || originalValue === null || originalValue === undefined) {
                patches.push({ op: 'add', path: `/${field.name}/${l.isoKey}`, value: currentValue });
              } else {
                patches.push({ op: 'replace', path: `/${field.name}/${l.isoKey}`, value: currentValue });
              }
            }
          }
        });
      } else {
        const currentValue = formValue[field.name];
        const originalValue = original[field.name];

        let compareCurrent = currentValue;
        let compareOriginal = originalValue;

        if (field.type === 'Number') {
          compareCurrent = currentValue ? parseFloat(currentValue) : null;
          compareOriginal = originalValue ? parseFloat(originalValue) : null;
        } else if (field.type === 'DateTime') {
          compareCurrent = currentValue ? new Date(currentValue).toISOString() : null;
          compareOriginal = originalValue ? new Date(originalValue).toISOString() : null;
        }

        if (compareCurrent !== compareOriginal) {
          if (currentValue === '' || currentValue === null || currentValue === undefined) {
            if (originalValue !== '' && originalValue !== null && originalValue !== undefined) {
              patches.push({ op: 'remove', path: `/${field.name}` });
            }
          } else {
            if (originalValue === '' || originalValue === null || originalValue === undefined) {
              patches.push({ op: 'add', path: `/${field.name}`, value: compareCurrent });
            } else {
              patches.push({ op: 'replace', path: `/${field.name}`, value: compareCurrent });
            }
          }
        }
      }
    });

    return patches;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
      this.cdr.detectChanges();
      return;
    }

    this.saving.set(true);
    this.error.set(null);

    const formValue = this.form.getRawValue();

    if (this.isEditMode()) {
      const patches = this.buildPatchOperations();

      if (patches.length === 0) {
        this.saving.set(false);
        this.router.navigate(['/' + this.lang(), 'generic', this.entityType().toLowerCase()]);
        return;
      }

      const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/${encodeURIComponent(this.id()!)}`;
      this.http.patch<any>(url, patches, {
        headers: { 'Content-Type': 'application/json-patch+json' }
      }).subscribe({
        next: (res) => {
          if (res.$messages && res.$messages.length > 0) {
            this.handleValidationErrors(res.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            this.router.navigate(['/' + this.lang(), 'generic', this.entityType().toLowerCase()]);
          }
        },
        error: (err) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to save form');
          }
          this.saving.set(false);
        }
      });
    } else {
      // POST Create
      const payload: any = {};
      const metaInfo = this.meta();

      if (metaInfo && metaInfo.fields) {
        metaInfo.fields.forEach(field => {
          if (field.type === 'LocalizedString') {
            const localizedMap: any = {};
            this.sessionService.availableLanguages().forEach(l => {
              const value = formValue[`${field.name}_${l.isoKey}`];
              if (value && value.trim()) {
                localizedMap[l.isoKey] = value;
              }
            });
            if (Object.keys(localizedMap).length > 0) {
              payload[field.name] = localizedMap;
            }
          } else if (field.type === 'Number') {
            const val = formValue[field.name];
            if (val !== '' && val !== null && val !== undefined) {
              payload[field.name] = parseFloat(val);
            }
          } else if (field.type === 'DateTime') {
            const val = formValue[field.name];
            if (val) {
              payload[field.name] = new Date(val).toISOString();
            }
          } else {
            const val = formValue[field.name];
            if (val !== '' && val !== null && val !== undefined) {
              payload[field.name] = val;
            }
          }
        });
      }

      const url = `${environment.apiBaseUrl}admin/api/${this.entityType().toLowerCase()}/create`;
      this.http.post<any>(url, payload).subscribe({
        next: (res) => {
          if (res.$messages && res.$messages.length > 0) {
            this.handleValidationErrors(res.$messages);
            this.saving.set(false);
          } else {
            this.saving.set(false);
            this.router.navigate(['/' + this.lang(), 'generic', this.entityType().toLowerCase()]);
          }
        },
        error: (err) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set(err.error?.message || 'Failed to create entity');
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
    this.router.navigate(['/' + this.lang(), 'generic', this.entityType().toLowerCase()]);
  }

  handleSaveKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
      return;
    }
    event.preventDefault();
    this.showSaveKeyHint.set(true);
    this.onSubmit();
    setTimeout(() => this.showSaveKeyHint.set(false), 500);
  }

  getPermissionEntityType(plural: string): string {
    const metaEntity = this.meta()?.entityType;
    if (metaEntity) {
      return metaEntity;
    }
    const lower = plural.toLowerCase();
    if (lower === 'currencies') return 'Currency';
    if (lower === 'channels') return 'Channel';
    if (lower === 'pricerows') return 'PriceRow';
    if (lower === 'groups') return 'Group';
    if (lower === 'units') return 'Unit';
    if (lower === 'taxclasses') return 'TaxClass';
    if (lower === 'languages') return 'Language';
    if (lower === 'countries') return 'Country';
    if (lower === 'organizations') return 'Organization';
    if (lower === 'app-roles') return 'AppRole';
    if (lower === 'app-permissions') return 'AppPermission';
    if (lower.endsWith('s')) {
      const singular = plural.slice(0, -1);
      return singular.charAt(0).toUpperCase() + singular.slice(1);
    }
    return plural.charAt(0).toUpperCase() + plural.slice(1);
  }

  getLabel(fieldName: string): string {
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1);
  }
}
