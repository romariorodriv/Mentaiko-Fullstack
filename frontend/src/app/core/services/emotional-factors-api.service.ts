import {Injectable,inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
export interface EmotionalFactor {id:number;code:string;label:string}
export interface FactorSummary {id:number;label:string;count:number}
export interface FactorsAnalysis {from:string;to:string;factors:FactorSummary[]}
@Injectable({providedIn:'root'})
export class EmotionalFactorsApi {
  private readonly http=inject(HttpClient);
  private readonly base=environment.apiUrl;
  catalog(){return this.http.get<EmotionalFactor[]>(`${this.base}/me/emotional-factors`);}
  selection(checkinId:number){return this.http.get<number[]>(`${this.base}/checkins/${checkinId}/factors`);}
  save(checkinId:number,factorIds:number[]){
    return this.http.put<number[]>(`${this.base}/checkins/${checkinId}/factors`,{factorIds});
  }
  analysis(from:string,to:string){
    return this.http.get<FactorsAnalysis>(`${this.base}/me/emotional-factors/analysis`,{params:{from,to}});
  }
}
