import { MetaInfo } from '../meta-info.model';

export interface AppPermission {
  id: number;
  name: string;
  description?: string;
  $info?: { [key: string]: any };
  $meta?: MetaInfo;
}

export interface AppPermissionList {
  items: AppPermission[];
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
