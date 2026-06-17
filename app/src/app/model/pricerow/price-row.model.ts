import { MetaInfo } from '../meta-info.model';

export interface PriceRow {
  id: string;
  pricedResourceId: string;
  priceValue: number;
  minQuantity: number;
  unitRef: string;
  currencyRef?: string;
  taxClassRef?: string;
  priceType?: string;
  taxIncluded: boolean;
  validFrom?: string;
  validTo?: string;
  channelRefs?: string[];
  groupRefs?: string[];
  $info?: {
    taxation?: {
      taxValue: number;
      taxRate: number;
      taxIncludedInfo: string;
    };
    groupRefIds?: { [path: string]: string };  // path → id map (read-only, for navigation)
    createdAt?: string;
    lastModifiedAt?: string;
  };
  $includes?: {
    unit?: {
      symbol: string;
      name: { [key: string]: string };
      measure: string;
      baseUnitRef?: string;
      factor: number;
    };
    currency?: {
      currencyKey: string;
      name: { [key: string]: string };
      symbol: string;
    };
    taxClass?: {
      taxClassId: string;
      taxRate: number;
      description?: { [key: string]: string };
    };
  };
  $meta?: MetaInfo;
}

export interface PriceRowList {
  items: PriceRow[];
  $info: {
    paging: {
      'page': number;
      'page-size': number;
      'total-items': number;
      'total-pages': number;
    };
  };
  $meta?: MetaInfo;
}
