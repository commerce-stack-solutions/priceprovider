import { Component, signal, inject, OnInit, ChangeDetectorRef, computed } from '@angular/core';

import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ChannelsService } from '../../../service/channel/channels.service';
import { Channel } from '../../../model/channel/channel.model';
import { MetaInfo } from '../../../model/meta-info.model';
import { CountriesService } from '../../../service/country/countries.service';
import { ReferenceListEditComponent, ReferenceDataSourceResult } from '../../../components/referencelist-edit/referencelist-edit.component';
import { EnumSelectorComponent } from '../../../components/enum-selector/enum-selector.component';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';
import { MessageTranslationService } from '../../../service/message-translation.service';
import { Message } from '../../../model/message.model';
import { IsMandatoryPipe } from '../../../pipes/is-mandatory.pipe';

const OPTIONS_PAGESIZE = 30;

@Component({
  selector: 'app-channel-form',
  templateUrl: './channel-form.component.html',
  styleUrls: ['./channel-form.component.scss'],
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, RouterModule, ReferenceListEditComponent, EnumSelectorComponent, TranslocoModule, IsMandatoryPipe],
  host: { '(document:keydown.s)': 'handleSaveKeyPress($event)' }
})
export class ChannelFormComponent implements OnInit {
  private channelsService = inject(ChannelsService);
  private countriesService = inject(CountriesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private sessionService = inject(SessionService);
  private permissionService = inject(PermissionService);
  private messageTranslationService = inject(MessageTranslationService);

  lang = computed(() => this.sessionService.language());

  form!: FormGroup;
  isEditMode = signal(false);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  channelId = signal<string | null>(null);
  fieldErrors = signal<Map<string, string[]>>(new Map());
  originalValues = signal<any>({});
  showSaveKeyHint = signal(false);
  meta = signal<MetaInfo | null>(null);
  priceRepresentationModeValues: string[] = [];

  countriesDataSource = (searchTerm: string, page: number): Observable<ReferenceDataSourceResult> => {
    const query = searchTerm ? `isoKey:*${searchTerm}*` : undefined;
    return this.countriesService.getCountries(page, OPTIONS_PAGESIZE, undefined, undefined, query).pipe(
      map(response => ({
        options: response.items.map(c => ({ value: c.isoKey, label: c.isoKey })),
        hasMore: response.$info.paging.page < response.$info.paging['total-pages'] - 1
      }))
    );
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.isEditMode.set(!!id);
    this.channelId.set(id);
    if (!this.permissionService.hasWritePermission('Channel')) {
      if (id) {
        this.router.navigate(['/' + this.lang(), 'channels', id]);
      } else {
        this.router.navigate(['/' + this.lang(), 'channels']);
      }
      return;
    }
    this.initForm();
    if (this.isEditMode()) {
      this.loadChannel(id!);
    } else {
      this.channelsService.getMeta().subscribe({
        next: (metaInfo) => {
          this.meta.set(metaInfo);
          if (metaInfo?.enumValues?.['priceRepresentationMode']) {
            this.priceRepresentationModeValues = metaInfo.enumValues['priceRepresentationMode'];
          }
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      id: [{ value: '', disabled: this.isEditMode() }, [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      allowedCountryRefs: [[]],
      priceRepresentationMode: [null, Validators.required]
    });
  }

  loadChannel(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.channelsService.getChannel(id).subscribe({
      next: (channel: Channel) => {
        if (channel.$meta) {
          this.meta.set(channel.$meta);
          if (channel.$meta.enumValues?.['priceRepresentationMode']) {
            this.priceRepresentationModeValues = channel.$meta.enumValues['priceRepresentationMode'];
          }
        }
        const patchData = {
          id: channel.id,
          allowedCountryRefs: channel.allowedCountryRefs || [],
          priceRepresentationMode: channel.priceRepresentationMode || null
        };
        this.form.patchValue(patchData);
        this.originalValues.set({ ...patchData });
        this.loading.set(false);
      },
      error: (err: any) => { this.error.set('Failed to load channel: ' + (err.message || 'Unknown error')); this.loading.set(false); }
    });
  }

  isMandatory(fieldName: string): boolean {
    const m = this.meta();
    if (m?.mandatoryFields) { return m.mandatoryFields.includes(fieldName); }
    return this.form?.get(fieldName)?.hasValidator(Validators.required) ?? false;
  }

  getPriceRepresentationModeDescription(mode: string | null): string {
    if (!mode) return '';
    const descriptions: { [key: string]: string } = {
      'NET_ONLY': 'Publish only prices that are already defined as net. Prices defined as gross are excluded.',
      'GROSS_ONLY': 'Publish only prices that are already defined as gross. Prices defined as net are excluded.',
      'FORCE_NET': 'Publish all prices as net. Any gross prices are converted to net before publishing.',
      'FORCE_GROSS': 'Publish all prices as gross. Any net prices are converted to gross before publishing.'
    };
    return descriptions[mode] || '';
  }

  buildPatchOperations(): any[] {
    const patches: any[] = [];
    const formValue = this.form.getRawValue();
    const original = this.originalValues();
    const currentRefs = formValue.allowedCountryRefs || [];
    const originalRefs = original.allowedCountryRefs || [];
    if (JSON.stringify(currentRefs) !== JSON.stringify(originalRefs)) {
      patches.push({ op: 'replace', path: '/allowedCountryRefs', value: currentRefs });
    }
    if (formValue.priceRepresentationMode !== original.priceRepresentationMode) {
      patches.push({ op: 'replace', path: '/priceRepresentationMode', value: formValue.priceRepresentationMode });
    }
    return patches;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(key => this.form.get(key)?.markAsTouched());
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
        this.router.navigate(['/' + this.lang(), 'channels', this.channelId()]);
        return;
      }
      this.channelsService.patchChannel(this.channelId()!, patches).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) { this.handleValidationErrors(response.$messages); this.saving.set(false); }
          else { this.saving.set(false); this.router.navigate(['/' + this.lang(), 'channels', this.channelId()]); }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) this.handleValidationErrors(err.error.$messages);
          else this.error.set(err.error?.message || 'Failed to save channel');
          this.saving.set(false);
        }
      });
    } else {
      const channel: Channel = {
        id: formValue.id,
        allowedCountryRefs: formValue.allowedCountryRefs || [],
        priceRepresentationMode: formValue.priceRepresentationMode
      };
      this.channelsService.createChannel(channel).subscribe({
        next: (response: any) => {
          if (response.$messages && response.$messages.length > 0) { this.handleValidationErrors(response.$messages); this.saving.set(false); }
          else { this.saving.set(false); this.router.navigate(['/' + this.lang(), 'channels', response.id]); }
        },
        error: (err: any) => {
          if (err.error && err.error.$messages && err.error.$messages.length > 0) this.handleValidationErrors(err.error.$messages);
          else this.error.set(err.error?.message || 'Failed to save channel');
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
            if (!fieldErrorsMap.has(field)) fieldErrorsMap.set(field, []);
            fieldErrorsMap.get(field)!.push(translatedMessage);
          });
        }
      }
    });
    this.fieldErrors.set(fieldErrorsMap);
    this.error.set(errorMessages.join('; '));
  }

  hasFieldError(fieldName: string): boolean { return this.fieldErrors().has(fieldName); }
  getFieldErrors(fieldName: string): string[] { return this.fieldErrors().get(fieldName) || []; }

  cancel(): void {
    if (this.isEditMode()) this.router.navigate(['/' + this.lang(), 'channels', this.channelId()]);
    else this.router.navigate(['/' + this.lang(), 'channels']);
  }

  handleSaveKeyPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) return;
    event.preventDefault();
    this.showSaveKeyHint.set(true);
    this.onSubmit();
    setTimeout(() => this.showSaveKeyHint.set(false), 500);
  }
}
