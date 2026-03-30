import { MetaInfo } from '../meta-info.model';

export interface Channel {
  id: string;
  allowedCountryRefs?: string[];
  priceRepresentationMode?: string;
  $info?: { [key: string]: any };
  $includes?: { [key: string]: any };
  $meta?: MetaInfo;
}
export interface ChannelList {
  items: Channel[];
  $info: { paging: { page: number; 'page-size': number; 'total-items': number; 'total-pages': number; }; sorting?: { 'sort-by': string[]; 'sort-direction': string; }; };
  $meta?: MetaInfo;
}
