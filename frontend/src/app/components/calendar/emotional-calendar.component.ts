import {Component, OnInit, inject, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {finalize} from 'rxjs';
import {environment} from '../../../environments/environment';
import { CheckinsService } from '../../core/services/checkins.service';
import {Checkin} from '../../shared/models/models';

interface EmotionCount {emotionId: number; emotion: string; count: number}
interface CalendarDay {date: string; total: number; emotions: EmotionCount[]}
interface CalendarMonth {year: number; month: number; days: CalendarDay[]}

@Component({
  selector: 'app-emotional-calendar',
  standalone: true,
  templateUrl: './emotional-calendar.component.html',
  styleUrl: './emotional-calendar.component.css',
  
})
export class EmotionalCalendarComponent implements OnInit {
  private readonly http=inject(HttpClient);
  private readonly checkinsApi=inject(CheckinsService);
  private selectionVersion=0;
  readonly weekdays=['L','M','X','J','V','S','D'];
  readonly loading=signal(false);
  readonly error=signal('');
  readonly selected=signal<CalendarDay|null>(null);
  readonly detailEntries=signal<Checkin[]>([]);
  readonly detailPage=signal(0);
  readonly detailPages=signal(0);
  readonly detailLoading=signal(false);
  readonly detailError=signal('');
  readonly cells=signal<(CalendarDay|null)[]>([]);
  readonly period=signal(new Date(new Date().getFullYear(),new Date().getMonth(),1));

  ngOnInit(){this.load();}
  monthLabel(){return new Intl.DateTimeFormat('es-PE',{month:'long',year:'numeric'}).format(this.period());}
  dayNumber(date:string){return Number(date.slice(-2));}
  dayLabel(day:CalendarDay){return `${day.date}: ${day.total} registros`;}
  changeMonth(delta:number) {
    const d=this.period();
    this.period.set(new Date(d.getFullYear(),d.getMonth()+delta,1));
    this.selected.set(null);
    this.selectionVersion++;
    this.detailEntries.set([]);
    this.load();
  }
  select(date:string) {
    const day=this.cells().find(d=>d?.date===date);
    if(day){
      this.selected.set(day);
      this.selectionVersion++;
      this.loadEntries(0);
    }
  }
  loadEntries(page:number){
    const day=this.selected();
    if(!day || page<0)return;
    const requestId=this.selectionVersion;
    this.detailLoading.set(true);this.detailError.set('');
    this.checkinsApi.checkins({from:day.date,to:day.date,page,size:10}).subscribe({
      next:result=>{
        if(requestId!==this.selectionVersion)return;
        this.detailEntries.set(result.content);
        this.detailPage.set(result.number);
        this.detailPages.set(result.totalPages);
        this.detailLoading.set(false);
      },
      error:()=>{
        if(requestId!==this.selectionVersion)return;
        this.detailLoading.set(false);
        this.detailError.set('No fue posible cargar el detalle del día.');
      }
    });
  }
  load() {
    const d=this.period();
    this.loading.set(true);this.error.set('');
    this.http.get<CalendarMonth>(`${environment.apiUrl}/me/emotional-calendar`,{
      params:{year:d.getFullYear(),month:d.getMonth()+1}
    }).pipe(finalize(()=>this.loading.set(false))).subscribe({
      next:data=>{
        const indexed=new Map(data.days.map(day=>[day.date,day]));
        const offset=(new Date(d.getFullYear(),d.getMonth(),1).getDay()+6)%7;
        const count=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
        const cells:(CalendarDay|null)[]=Array(offset).fill(null);
        for(let n=1;n<=count;n++){
          const date=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(n).padStart(2,'0')}`;
          cells.push(indexed.get(date)??{date,total:0,emotions:[]});
        }
        this.cells.set(cells);
      },
      error:()=>this.error.set('No pudimos cargar el calendario emocional.')
    });
  }
}
