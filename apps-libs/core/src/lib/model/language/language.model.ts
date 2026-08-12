import { MetaInfo } from '../meta-info.model';

export interface Language {
  isoKey: string;
  active?: boolean;
  mandatory?: boolean;
  name?: { [key: string]: string };
  $info?: {
    [key: string]: any;
  };
  $includes?: {
    [key: string]: any;
  };
  $meta?: MetaInfo;
}

export interface LanguageList {
  items: Language[];
  $info?: {
    paging?: {
      page: number;
      'page-size': number;
      'total-items': number;
      'total-pages': number;
    };
  };
  $meta?: MetaInfo;
}
