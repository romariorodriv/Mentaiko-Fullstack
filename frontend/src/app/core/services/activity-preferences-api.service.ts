import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export interface ActivityPreferences {
  selected:string[];
  available:string[];
}
@Injectable({providedIn:'root'})
export class ActivityPreferencesApi {
  private readonly http=inject(HttpClient);
  private readonly url=`${environment.apiUrl}/me/activity-preferences`;
  get(){return this.http.get<ActivityPreferences>(this.url);}
  save(selected:string[]){return this.http.put<ActivityPreferences>(this.url,{selected});}
}
