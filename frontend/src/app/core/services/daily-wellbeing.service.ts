import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export interface SleepEntry {date: string; hours: number; quality: number}
export interface EnergyEntry {date: string; level: number}

@Injectable({providedIn:'root'})
export class DailyWellbeingApi {
  private readonly http=inject(HttpClient);
  private readonly api=environment.apiUrl;
  sleepHistory() {return this.http.get<SleepEntry[]>(`${this.api}/me/sleep`);}
  saveSleep(date:string,hours:number,quality:number) {
    return this.http.put<SleepEntry>(`${this.api}/me/sleep/${date}`,{hours,quality});
  }
  energyHistory() {return this.http.get<EnergyEntry[]>(`${this.api}/me/energy`);}
  saveEnergy(date:string,level:number) {
    return this.http.put<EnergyEntry>(`${this.api}/me/energy/${date}`,{level});
  }
}
