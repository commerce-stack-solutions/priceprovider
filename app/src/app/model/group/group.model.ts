import { MetaInfo } from '../meta-info.model';

export interface Group {
  id?: string;   // generated string ID (auto-assigned, read-only after creation)
  path?: string; // Unique human-readable identifier (required for create)
  name?: string;
  parentRefs?: string[];  // path values
  subRefs?: string[];     // path values
  parentRefIds?: { [path: string]: string };  // path → id map (read-only, for navigation)
  subRefIds?: { [path: string]: string };     // path → id map (read-only, for navigation)
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
