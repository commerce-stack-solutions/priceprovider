import { MetaInfo } from '../meta-info.model';

export interface TaxClass {
  taxClassId: string;
  taxRate: number;
  countryRef?: string;
  $info?: {
    [key: string]: any;
  };
  $includes?: {
    [key: string]: any;
  };
  $meta?: MetaInfo;
}

export interface TaxClassList {
  items: TaxClass[];
  $info: {
    paging: {
      page: number;
      'page-size': number;
      'total-items': number;
      'total-pages': number;
    };
    sorting?: {
      'sort-by': string[];
      'sort-direction': string;
    };
  };
  $meta?: MetaInfo;
}
