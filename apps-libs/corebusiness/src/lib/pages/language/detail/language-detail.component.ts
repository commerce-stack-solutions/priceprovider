import { Component, signal, inject, OnInit, computed } from '@angular/core';

import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { LanguagesService } from '../../../service/language/languages.service';
import { Language } from '../../../model/language/language.model';
import { InfoSectionComponent, InfoSection, InfoField } from '../../../components/info-section/info-section.component';
import { DateTimeService } from '../../../service/datetime.service';
import { LabelService } from '../../../service/label.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { SessionService } from '../../../service/session.service';
import { LocalizedStringfieldViewComponent } from '../../../components/localized-stringfield-view/localized-stringfield-view.component';
import { PermissionService } from '../../../service/permission.service';

@Component({
  selector: 'app-language-detail',
  templateUrl: './language-detail.component.html',
  styleUrls: ['./language-detail.component.scss'],
  standalone: true,
  imports: [RouterModule, InfoSectionComponent, TranslocoModule, LocalizedStringfieldViewComponent],
  host: {
    '(document:keydown.e)': 'handleEditKeyPress($event)'
  }
})
export class LanguageDetailComponent implements OnInit {
  private languagesService = inject(LanguagesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dateTime = inject(DateTimeService);
  private label = inject(LabelService);
  private sessionService = inject(SessionService);
  private transloco = inject(TranslocoService);
  protected permissionService = inject(PermissionService);

  lang = computed(() => this.sessionService.language());

  language = signal<Language | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  showEditKeyHint = signal(false);

  canWrite = computed(() => this.permissionService.hasWritePermission('Language'));
  canDelete = computed(() => this.permissionService.hasDeletePermission('Language'));

  // Computed property for info sections
  infoSections = computed<InfoSection[]>(() => {
    const o = this.language();
    if (!o || !o.$info) return [];

    const allInfoKeys = Object.keys(o.$info);
    if (allInfoKeys.length === 0) return [];

    const sections: InfoSection[] = [];

    // Audit Information section
    if (o.$info['createdAt'] || o.$info['lastModifiedAt']) {
      const fields: InfoField[] = [];
      const createdAt = o.$info['createdAt'];
      if (createdAt) {
        fields.push({ label: 'Created At', value: this.dateTime.formatDate(createdAt), type: 'text' });
      }
      if (o.$info['lastModifiedAt']) {
        fields.push({ label: 'Last Modified At', value: this.dateTime.formatDate(o.$info['lastModifiedAt']), type: 'text' });
      }
      sections.push({
        title: 'Audit Information',
        fields
      });
    }

    // Other info fields section (excluding createdAt and lastModifiedAt)
    const otherInfoKeys = allInfoKeys.filter(k => k !== 'createdAt' && k !== 'lastModifiedAt');
    if (otherInfoKeys.length > 0) {
      const fields: InfoField[] = otherInfoKeys.map(key => ({
        label: this.label.formatLabel(key),
        value: typeof o.$info![key] === 'object' ? JSON.stringify(o.$info![key]) : String(o.$info![key]),
        type: 'text' as const
      }));
      sections.push({
        title: 'Other Information',
        fields
      });
    }

    return sections;
  });

  ngOnInit(): void {
    const isoKey = this.route.snapshot.paramMap.get('isoKey');
    if (isoKey) {
      this.loadLanguage(isoKey);
    } else {
      this.error.set('Language ISO key not provided');
      this.loading.set(false);
    }
  }

  loadLanguage(isoKey: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.languagesService.getLanguage(isoKey).subscribe({
      next: (data: Language) => {
        this.language.set(data);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.error.set('Failed to load language: ' + (err.message || 'Unknown error'));
        this.loading.set(false);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/' + this.lang(), 'languages']);
  }

  editLanguage(): void {
    if (!this.canWrite()) return;
    const lang = this.language();
    if (lang) {
      this.router.navigate(['/' + this.lang(), 'languages', lang.isoKey, 'edit']);
    }
  }

  deleteLanguage(): void {
    if (!this.canDelete()) return;
    const lang = this.language();
    if (!lang) return;

    if (confirm(this.transloco.translate('common.messages.confirmDelete'))) {
      this.languagesService.deleteLanguage(lang.isoKey).subscribe({
        next: () => {
          this.router.navigate(['/languages']);
        },
        error: (error) => {
          this.error.set(this.transloco.translate('common.errors.language.deleteError'));
          console.error('Error deleting language:', error);
        }
      });
    }
  }

  handleEditKeyPress(event: Event): void {
    // Only handle if language is loaded and not already on an input field
    if (!(event instanceof KeyboardEvent)) return;
    const target = event.target as HTMLElement | null;
    if (!this.language() || (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'))) {
      return;
    }

    // Show visual feedback
    this.showEditKeyHint.set(true);

    // Navigate to edit page
    const lang = this.language();
    if (lang && this.canWrite()) {
      this.router.navigate(['/' + this.lang(), 'languages', lang.isoKey, 'edit']);
    }
  }

  getLocalizedName(name: { [key: string]: string } | undefined): string {
    if (!name) return '-';
    const lang = this.sessionService.language();
    return name[lang] || name['en'] || Object.values(name)[0] || '-';
  }
}
