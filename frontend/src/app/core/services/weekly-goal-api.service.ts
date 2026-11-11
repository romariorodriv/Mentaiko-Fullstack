import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export interface WeeklyGoal {
  weekStart: string;
  targetDays: number|null;
  completedDays: number;
  percentage: number;
  status: 'SIN_META'|'PENDIENTE'|'EN_PROGRESO'|'CUMPLIDA';
}

@Injectable({providedIn:'root'})
export class WeeklyGoalApi {
  private readonly http=inject(HttpClient);
  private readonly url=`${environment.apiUrl}/me/weekly-goal`;
  get() {return this.http.get<WeeklyGoal>(this.url);}
  save(targetDays:number) {return this.http.put<WeeklyGoal>(this.url,{targetDays});}
}
