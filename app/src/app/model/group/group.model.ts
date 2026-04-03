import { MetaInfo } from '../meta-info.model';

export interface Group {
  id?: string;   // UUID (auto-generated, read-only after creation)
  path?: string; // Unique human-readable identifier (required for create)
  name?: string;
  parentRefs?: string[];  // path values
  subRefs?: string[];     // path values
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
