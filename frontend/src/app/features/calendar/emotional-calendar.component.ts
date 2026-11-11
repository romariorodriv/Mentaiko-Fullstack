import {Component, OnInit, inject, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {finalize} from 'rxjs';
import {environment} from '../../../environments/environment';

interface EmotionCount {emotionId: number; emotion: string; count: number}
interface CalendarDay {date: string; total: number; emotions: EmotionCount[]}
interface CalendarMonth {year: number; month: number; days: CalendarDay[]}

@Component({
  selector: 'app-emotional-calendar',
  standalone: true,
  template: `
  <header class="page-head"><div><span class="eyebrow">TU HISTORIA</span>
    <h1>Calendario emocional</h1>
    <p>Revisa los días en los que registraste cómo te sentías.</p></div></header>
  <section class="panel">
    <div class="calendar-toolbar">
      <button type="button" class="btn" (click)="changeMonth(-1)" aria-label="Mes anterior">←</button>
      <h2 aria-live="polite">{{monthLabel()}}</h2>
      <button type="button" class="btn" (click)="changeMonth(1)" aria-label="Mes siguiente">→</button>
    </div>
    @if(loading()) {<p role="status">Cargando calendario...</p>}
    @if(error()) {<p class="alert error" role="alert">{{error()}} <button (click)="load()">Reintentar</button></p>}
    <div class="calendar-grid" role="group" aria-label="Días del mes">
      @for(label of weekdays; track label) {<strong class="calendar-weekday">{{label}}</strong>}
      @for(cell of cells(); track $index) {
        @if(cell) {
          <button type="button" class="calendar-day" [class.selected]="selected()?.date===cell.date"
                  (click)="select(cell.date)" [attr.aria-label]="dayLabel(cell)"
                  [attr.aria-pressed]="selected()?.date===cell.date">
            <span>{{dayNumber(cell.date)}}</span>
            @if(cell.total>0){<small>{{cell.total}} registro{{cell.total===1?'':'s'}}</small>}
          </button>
        } @else {<span aria-hidden="true"></span>}
      }
    </div>
    @if(selected(); as day) {
      <section class="calendar-detail" aria-live="polite">
        <h3>{{day.date}}</h3>
        @if(day.total===0){<p>No hay registros emocionales en este día.</p>}
        @for(item of day.emotions; track item.emotionId){
          <p>{{item.emotion}}: {{item.count}} registro{{item.count===1?'':'s'}}</p>
        }
      </section>
    }
  </section>
  `,
  styles: [`
    .calendar-toolbar{display:flex;align-items:center;justify-content:space-between;gap:1rem;margin-bottom:1.2rem}
    .calendar-toolbar h2{margin:0;text-align:center}
    .calendar-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:.35rem}
    .calendar-weekday{text-align:center;padding:.4rem 0;font-size:.8rem}
    .calendar-day{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:.3rem;min-height:4rem;border:1px solid #ccd3dd;border-radius:.6rem;background:transparent;cursor:pointer;font:inherit}
    .calendar-day small{font-size:.7rem;text-align:center}
    .calendar-day.selected{outline:2px solid #4168a2}
    .calendar-day:focus-visible{outline:3px solid #4168a2}
    .calendar-detail{margin-top:1.2rem;border-top:1px solid #ccd3dd;padding-top:1rem}
    @media(max-width:600px){.calendar-grid{gap:.1rem}.calendar-day{min-height:3.2rem;padding:.1rem}.calendar-day small{font-size:.6rem}}
  `]
})
export class EmotionalCalendarComponent implements OnInit {
  private readonly http=inject(HttpClient);
  readonly weekdays=['L','M','X','J','V','S','D'];
  readonly loading=signal(false);
  readonly error=signal('');
  readonly selected=signal<CalendarDay|null>(null);
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
    this.load();
  }
  select(date:string) {
    const day=this.cells().find(d=>d?.date===date);
    if(day) this.selected.set(day);
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
