import { MetaInfo } from '../meta-info.model';

export interface Currency {
  currencyKey: string;
  symbol: string;
  name?: { [key: string]: string };
  $info?: {
    [key: string]: any;
  };
  $includes?: {
    [key: string]: any;
  };
  $meta?: MetaInfo;
}

export interface CurrencyList {
  items: Currency[];
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
