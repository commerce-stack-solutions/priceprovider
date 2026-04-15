import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { convertToParamMap } from '@angular/router';
import { of, firstValueFrom } from 'rxjs';
import { PricerowFormComponent } from './pricerow-form.component';
import { PricerowsService } from '../../../service/pricerow/pricerows.service';
import { CurrenciesService } from '../../../service/currency/currencies.service';
import { UnitsService } from '../../../service/unit/units.service';
import { TaxClassesService } from '../../../service/taxclass/taxclasses.service';
import { GroupsService } from '../../../service/group/groups.service';
import { ChannelsService } from '../../../service/channel/channels.service';
import { ModalService } from '../../../service/modal.service';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../../service/session.service';
import { PermissionService } from '../../../service/permission.service';
import { MessageTranslationService } from '../../../service/message-translation.service';

describe('PricerowFormComponent', () => {
  let component: PricerowFormComponent;
  let taxClassesServiceMock: jasmine.SpyObj<TaxClassesService>;

  beforeEach(() => {
    taxClassesServiceMock = jasmine.createSpyObj<TaxClassesService>('TaxClassesService', ['getTaxClasses']);

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        { provide: PricerowsService, useValue: {} },
        { provide: CurrenciesService, useValue: {} },
        { provide: UnitsService, useValue: {} },
        { provide: TaxClassesService, useValue: taxClassesServiceMock },
        { provide: GroupsService, useValue: {} },
        { provide: ChannelsService, useValue: {} },
        { provide: ModalService, useValue: {} },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
        { provide: SessionService, useValue: { language: () => 'en' } },
        { provide: PermissionService, useValue: { hasWritePermission: () => true } },
        { provide: MessageTranslationService, useValue: { translateMessage: (message: { message?: string }) => message.message ?? '' } }
      ]
    });

    component = TestBed.runInInjectionContext(() => new PricerowFormComponent());
  });

  it('formats tax class options using taxRate as percentage value', async () => {
    taxClassesServiceMock.getTaxClasses.and.returnValue(of({
      items: [{ taxClassId: 'de-vat-full', taxRate: 19 }],
      $info: { paging: { page: 0, 'total-pages': 1 } }
    } as any));

    const result = await firstValueFrom(component.taxClassesDataSource('', 0));

    expect(result.options[0].label).toBe('de-vat-full (19.00%)');
  });
});
