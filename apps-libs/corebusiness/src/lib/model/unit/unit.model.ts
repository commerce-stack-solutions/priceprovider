import { MetaInfo } from '../meta-info.model';

export interface Unit {
  symbol: string;
  name: { [key: string]: string };
  measure?: string;
  baseUnitRef?: string;
  factor?: number;
  $info?: {
    [key: string]: any;
  };
  $includes?: {
    baseUnit?: Unit;
    [key: string]: any;
  };
  $meta?: MetaInfo;
}

export interface UnitList {
  items: Unit[];
  $info: {
    paging: {
      page: number;
      "page-size": number;
      "total-items": number;
      "total-pages": number;
    };
  };
  $meta?: MetaInfo;
}
