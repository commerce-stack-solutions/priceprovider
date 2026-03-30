import { MetaInfo } from '../meta-info.model';

export interface Group {
  id: string;
  name?: string;
  parentRefs?: string[];
  subRefs?: string[];
  $info?: {
    [key: string]: any;
  };
  $includes?: {
    [key: string]: any;
  };
  $meta?: MetaInfo;
}

export interface GroupList {
  items: Group[];
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
