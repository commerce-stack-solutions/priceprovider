import { Pipe, PipeTransform } from '@angular/core';
import { MetaInfo } from '../model/meta-info.model';

@Pipe({
  name: 'isMandatory',
  standalone: true,
  pure: true,
})
export class IsMandatoryPipe implements PipeTransform {
  transform(fieldName: string, meta: MetaInfo | null | undefined): boolean {
    return meta?.mandatoryFields?.includes(fieldName) ?? false;
  }
}
