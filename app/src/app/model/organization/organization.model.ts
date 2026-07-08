import { Group } from '../group/group.model';
import { MetaInfo } from '../meta-info.model';

export enum OrganizationType {
  HOST_ORGANIZATION = 'HOST_ORGANIZATION',
  COMPANY = 'COMPANY',
  BUSINESS_UNIT = 'BUSINESS_UNIT',
  PUBLIC_INSTITUTION = 'PUBLIC_INSTITUTION',
  DEPARTMENT = 'DEPARTMENT',
  NON_PROFIT_ORGANIZATION = 'NON_PROFIT_ORGANIZATION'
}

export interface Organization extends Group {
  organizationType?: OrganizationType;
}

export interface OrganizationList {
  items: Organization[];
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
