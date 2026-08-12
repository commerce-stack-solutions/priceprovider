import { MetaInfo } from '../meta-info.model';

export interface Country {
  isoKey: string;
  name?: { [key: string]: string };
  allowedCurrencyRefs?: string[];
  primaryCurrencyRef?: string;
  $info?: { [key: string]: any };
  $includes?: { [key: string]: any };
  $meta?: MetaInfo;
}
export interface CountryList {
  items: Country[];
  $info: { paging: { page: number; 'page-size': number; 'total-items': number; 'total-pages': number; }; sorting?: { 'sort-by': string[]; 'sort-direction': string; }; };
  $meta?: MetaInfo;
}
