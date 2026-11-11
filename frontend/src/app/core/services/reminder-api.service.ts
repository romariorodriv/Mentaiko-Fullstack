import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export interface ReminderPreference {
  enabled:boolean;
  time:string;
  days:number[];
  due:boolean;
}
@Injectable({providedIn:'root'})
export class ReminderApi {
  private readonly http=inject(HttpClient);
  private readonly url=`${environment.apiUrl}/me/reminders`;
  get(){return this.http.get<ReminderPreference>(this.url);}
  update(enabled:boolean,time:string,days:number[]){
    return this.http.put<ReminderPreference>(this.url,{enabled,time,days});
  }
  acknowledge(){return this.http.post<ReminderPreference>(`${this.url}/acknowledge`,{});}
}
