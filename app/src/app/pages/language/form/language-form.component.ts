import { Component, signal, inject, OnInit, computed } from '@angular/core';

import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { LanguagesService } from '../../../service/language/languages.service';
import { Language } from '../../../model/language/language.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { TranslocoModule } from '@jsverse/transloco';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { LocalizedStringfieldEditComponent } from '../../../components/localized-stringfield-edit/localized-stringfield-edit.component';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

@Component({
  selector: 'app-language-form',
  templateUrl: './language-form.component.html',
  styleUrls: ['./language-form.component.scss'],
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, RouterModule, TranslocoModule, LocalizedStringfieldEditComponent, IsMandatoryPipe],
  host: {
    '(document:keydown.s)': 'handleSaveKeyPress($event)'
  }
})
export class LanguageFormComponent implements OnInit {
  private languagesService = inject(LanguagesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);
  private fb = inject(FormBuilder);

  lang = computed(() => this.sessionService.language());

  form!: FormGroup;
  isEditMode = signal(false);
  loading = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  originalLanguage = signal<Language | null>(null);
  meta = signal<MetaInfo | null>(null);
  
  isoKey = '';
  showSaveKeyHint = signal(false);

  // Language management for localized values
  languages = signal<string[]>([]);
  
  // Computed values for mandatory, active, and inactive languages
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

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  isLanguageMandatory(lang: string): boolean {
    return this.mandatoryLanguages().includes(lang);
  }
  
  isInactive(lang: string): boolean {
    return this.inactiveLanguages().includes(lang);
  }

  ngOnInit(): void {
    const isoKey = this.route.snapshot.paramMap.get('isoKey');
    this.isEditMode.set(!!isoKey);
    if (isoKey) {
      this.isoKey = isoKey;
    }

    if (!this.permissionService.hasWritePermission('Language')) {
      if (isoKey) {
        this.router.navigate(['/' + this.lang(), 'languages', isoKey]);
      } else {
        this.router.navigate(['/' + this.lang(), 'languages']);
      }
      return;
    }

    // Wait for languages to be loaded before initializing form
    if (this.sessionService.availableLanguages().length > 0) {
      this.initFormAndLoadData(isoKey);
    } else {
      // Wait a bit for languages to load
      setTimeout(() => {
        this.initFormAndLoadData(isoKey);
      }, 100);
    }
  }

  private initFormAndLoadData(isoKey: string | null): void {
    this.initForm();

    if (this.isEditMode() && isoKey) {
      this.loadLanguage(isoKey);
    } else {
      // When adding new language, show mandatory + active languages
      this.languages.set([...this.mandatoryLanguages(), ...this.activeLanguages()]);
      this.languagesService.getMeta().subscribe({
        next: (metaInfo) => { this.meta.set(metaInfo); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    // Create a FormGroup for the main form controls
    const formConfig: any = {
      isoKey: [{ value: '', disabled: this.isEditMode() }, [Validators.required, Validators.pattern(/^[a-z]{2,10}$/), Validators.maxLength(10)]],
      active: [true],
      mandatory: [false]
    };

    // Add form controls for all languages (active + inactive)
    const allLanguages = this.sessionService.availableLanguages().map(l => l.isoKey);
    
    allLanguages.forEach(lang => {
      const isMandatory = this.mandatoryLanguages().includes(lang);
      formConfig[`name_${lang}`] = ['', isMandatory ? Validators.required : []];
    });
    this.form = this.fb.group(formConfig);
  }

  loadLanguage(isoKey: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.languagesService.getLanguage(isoKey).subscribe({
      next: (data: Language) => {
        // Store original language to preserve invisible languages
        this.originalLanguage.set(data);
        if (data.$meta) { this.meta.set(data.$meta); }
        
        // Build patch data with all fields including language values
        const patchData: any = {
          isoKey: data.isoKey,
          active: data.active ?? true,
          mandatory: data.mandatory ?? false
        };
        
        // Patch all language values from the language
        if (data.name) {
          Object.keys(data.name).forEach(lang => {
            patchData[`name_${lang}`] = data.name![lang];
          });
        }
        
        this.form.patchValue(patchData);
        
        // Store original values for change tracking
        this.originalValues.set({ ...patchData });
        
        // Build list of languages to display: mandatory + active + inactive with values
        const languagesToDisplay = [...this.mandatoryLanguages(), ...this.activeLanguages()];
        
        // Add inactive languages that have values
        if (data.name) {
          this.inactiveLanguages().forEach(lang => {
            if (data.name![lang] && data.name![lang].trim() !== '' && !languagesToDisplay.includes(lang)) {
              languagesToDisplay.push(lang);
            }
          });
        }
        
        this.languages.set(languagesToDisplay);
        
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load language: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  removeLanguage(lang: string): void {
    // Cannot remove mandatory languages or inactive languages
    if (this.isLanguageMandatory(lang) || this.isInactive(lang)) return;
    
    const current = this.languages();
    this.languages.set(current.filter(l => l !== lang));
    // Clear the form field value when user explicitly removes a language
    const controlName = `name_${lang}`;
    const control = this.form.get(controlName);
    if (control) {
      control.setValue('', { emitEvent: true });
    }
  }

  addLanguage(lang: string): void {
    const current = this.languages();
    if (!current.includes(lang)) {
      this.languages.set([...current, lang]);
    }
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();
    
    if (formValue.active !== original.active) {
      patches.push({ op: 'replace', path: '/active', value: formValue.active });
    }
    if (formValue.mandatory !== original.mandatory) {
      patches.push({ op: 'replace', path: '/mandatory', value: formValue.mandatory });
    }
    
    // Check name fields (localized strings)
    Object.keys(formValue).forEach(key => {
      if (key.startsWith('name_')) {
        const lang = key.substring(5);
        const currentValue = formValue[key] || '';
        const originalValue = original[key] || '';
        
        if (currentValue !== originalValue) {
          if (currentValue === '' || currentValue === null) {
            // Only send remove if field existed in original
            if (originalValue !== '' && originalValue !== null && originalValue !== undefined) {
              patches.push({ op: 'remove', path: `/name/${lang}` });
            }
          } else {
            // Use 'add' if field didn't exist in original, 'replace' if it did
            if (originalValue === '' || originalValue === null || originalValue === undefined) {
              patches.push({ op: 'add', path: `/name/${lang}`, value: currentValue });
            } else {
              patches.push({ op: 'replace', path: `/name/${lang}`, value: currentValue });
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
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.form.getRawValue();

    if (this.isEditMode()) {
      // Use PATCH for updates
      const patches = this.buildPatchOperations();
      
      if (patches.length === 0) {
        // No changes, just navigate to detail view
        this.loading.set(false);
        this.router.navigate(['/' + this.lang(), 'languages', this.isoKey]);
        return;
      }
      
      this.languagesService.patchLanguage(this.isoKey, patches).subscribe({
        next: (response: any) => {
          // Check if response contains error messages
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.loading.set(false);
          } else {
            // Navigate to detail view after successful save
            this.loading.set(false);
            this.router.navigate(['/' + this.lang(), 'languages', this.isoKey]);
          }
        },
        error: (err: any) => {
          // Handle HTTP error responses
          if (err.error && err.error.$messages) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set('Failed to save language: ' + (err.message || 'Unknown error'));
          }
          this.loading.set(false);
        }
      });
    } else {
      // Use POST for creates
      // Build name object from form fields
      const name: { [key: string]: string } = {};
      
      Object.keys(formValue).forEach(key => {
        if (key.startsWith('name_')) {
          const lang = key.substring(5); // Remove "name_" prefix
          const value = formValue[key];
          if (value && value.trim()) {
            name[lang] = value;
          }
        }
      });

      const language: Language = {
        isoKey: formValue.isoKey,
        active: formValue.active,
        mandatory: formValue.mandatory,
        name: Object.keys(name).length > 0 ? name : undefined
      };

      this.languagesService.createLanguage(language).subscribe({
        next: (response: any) => {
          // Check if response contains error messages
          if (response.$messages && response.$messages.length > 0) {
            this.handleValidationErrors(response.$messages);
            this.loading.set(false);
          } else {
            this.router.navigate(['/' + this.lang(), 'languages', response.isoKey]);
          }
        },
        error: (err: any) => {
          // Handle HTTP error responses
          if (err.error && err.error.$messages) {
            this.handleValidationErrors(err.error.$messages);
          } else {
            this.error.set('Failed to save language: ' + (err.message || 'Unknown error'));
          }
          this.loading.set(false);
        }
      });
    }
  }
  
  handleValidationErrors(messages: any[]): void {
    const fieldErrorsMap = new Map<string, string[]>();
    const errorMessages: string[] = [];

    messages.forEach((msg: Message) => {
      if (msg.type === 'ERROR') {
        // Translate the message using the message translation service
        const translatedMessage = this.messageTranslationService.translateMessage(msg);
        errorMessages.push(translatedMessage);
        
        // If fields are specified, map them to the error message
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

  goBack(): void {
      this.router.navigate(['/' + this.lang(), 'languages']);
  }

  cancel(): void {
    if (this.isEditMode()) {
      this.router.navigate(['/' + this.lang(), 'languages', this.isoKey]);
    } else {
      this.router.navigate(['/' + this.lang(), 'languages']);
    }
  }

  handleSaveKeyPress(event: Event): void {
    // Ensure keyboard event and guard against input/textarea targets
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
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
}
