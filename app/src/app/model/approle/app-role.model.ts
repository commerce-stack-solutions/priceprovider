import { MetaInfo } from '../meta-info.model';

export interface AppRole {
  id: number;
  name: string;
  description?: string;
  permissionRefs?: string[];
  $info?: {
    permissionRefIds?: { [name: string]: number };
    [key: string]: any;
  };
  $meta?: MetaInfo;
}

export interface AppRoleList {
  items: AppRole[];
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
