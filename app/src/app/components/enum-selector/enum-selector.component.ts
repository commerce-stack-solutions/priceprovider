import { Component, Input, signal, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface EnumOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-enum-selector',
  templateUrl: './enum-selector.component.html',
  styleUrls: ['./enum-selector.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: EnumSelectorComponent,
    multi: true
  }]
})
export class EnumSelectorComponent implements ControlValueAccessor, OnInit {
  @Input() label: string = '';
  @Input() placeholder: string = 'Select...';
  @Input() options: EnumOption[] = [];
  @Input() set values(vals: string[]) {
    this.options = vals.map(v => ({ value: v, label: this.formatLabel(v) }));
  }
  @Input() isInvalid: boolean = false;
  @Input() required: boolean = false;

  selectedValue = signal<string | null>(null);
  disabled = false;

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};
  private isInitializing = true;

  constructor() {
    // Notify form control when value changes (but not during initialization)
    effect(() => {
      const value = this.selectedValue();
      if (!this.isInitializing) {
        this.onChange(value);
      }
    });
  }

  ngOnInit(): void {
    // Mark initialization complete after first render
    setTimeout(() => {
      this.isInitializing = false;
    }, 0);
  }

  // ControlValueAccessor implementation
  writeValue(value: string | null): void {
    this.selectedValue.set(value);
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onValueChange(value: string | null): void {
    // Handle empty string as null
    const normalizedValue = value === '' || value === null ? null : value;
    this.selectedValue.set(normalizedValue);
    this.onTouched();
  }

  private formatLabel(value: string): string {
    return value.split('_').map(word => 
      word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
  }

  getDisplayValue(): string {
    const value = this.selectedValue();
    if (!value) {
      return '';
    }
    const option = this.options.find(opt => opt.value === value);
    return option ? option.label : value;
  }
}
