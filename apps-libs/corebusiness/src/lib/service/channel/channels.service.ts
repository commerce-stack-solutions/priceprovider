import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Channel, ChannelList } from '../../model/channel/channel.model';
import { MetaInfo } from '../../model/meta-info.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChannelsService {
  private http = inject(HttpClient);

  getChannels(page: number, pageSize: number, sortBy?: string[], sortDirection?: string, query?: string): Observable<ChannelList> {
    let params = new HttpParams().set('page', page.toString()).set('page-size', pageSize.toString());
    if (sortBy && sortBy.length > 0) { sortBy.forEach(field => { params = params.append('sort-by', field); }); if (sortDirection) { params = params.set('sort-direction', sortDirection); } }
    if (query) { params = params.set('q', query); }
    return this.http.get<ChannelList>(`${environment.apiBaseUrl}admin/api/channels`, { params });
  }
  getChannel(id: string): Observable<Channel> {
    return this.http.get<Channel>(`${environment.apiBaseUrl}admin/api/channels/${encodeURIComponent(id)}`, { params: new HttpParams().set('$expand', '$includes,$info,$meta') });
  }
  getMeta(): Observable<MetaInfo> {
    return this.http.get<MetaInfo>(`${environment.apiBaseUrl}admin/api/channels/$meta`);
  }
  createChannel(channel: Channel): Observable<Channel> { return this.http.post<Channel>(`${environment.apiBaseUrl}admin/api/channels/create`, channel); }
  updateChannel(id: string, channel: Channel): Observable<Channel> { return this.http.put<Channel>(`${environment.apiBaseUrl}admin/api/channels/${encodeURIComponent(id)}`, channel); }
  patchChannel(id: string, patch: any[]): Observable<Channel> { return this.http.patch<Channel>(`${environment.apiBaseUrl}admin/api/channels/${encodeURIComponent(id)}`, patch, { headers: { 'Content-Type': 'application/json-patch+json' } }); }
  deleteChannel(id: string): Observable<void> { return this.http.delete<void>(`${environment.apiBaseUrl}admin/api/channels/${encodeURIComponent(id)}`); }
  bulkDeleteChannels(ids: string[]): Observable<void> { return this.http.post<void>(`${environment.apiBaseUrl}admin/api/channels/bulk-delete`, ids); }
}
