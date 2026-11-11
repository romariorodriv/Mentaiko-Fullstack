import {Component, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {DailyWellbeingApi, SleepEntry} from '../../core/services/daily-wellbeing.service';

@Component({
  selector:'app-sleep',
  standalone:true,
  imports:[FormsModule],
  template:`
    <header class="page-head"><div><span class="eyebrow">TU DESCANSO</span><h1>Registro de sueño</h1>
      <p>Registra las horas dormidas y cómo valoras tu descanso. La fecha corresponde al día en que despertaste.</p></div></header>
    <section class="panel">
      <form (ngSubmit)="save()">
        <label>Fecha de despertar <input type="date" name="date" [(ngModel)]="date" required [max]="today"></label>
        <label>Horas dormidas <input type="number" name="hours" [(ngModel)]="hours" required min="0" max="24" step=".25"></label>
        <label>Calidad de sueño
          <select name="quality" [(ngModel)]="quality" required>
            <option [ngValue]="1">1 — Muy mala</option><option [ngValue]="2">2 — Mala</option>
            <option [ngValue]="3">3 — Regular</option><option [ngValue]="4">4 — Buena</option>
            <option [ngValue]="5">5 — Muy buena</option>
          </select>
        </label>
        <button class="btn primary" type="submit" [disabled]="saving()">Guardar descanso</button>
        <p>Si ya existe un registro para esta fecha, se actualizará.</p>
      </form>
      @if(message()){<p class="alert success" role="status">{{message()}}</p>}
      @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
    </section>
    <section class="panel"><h2>Últimos 30 días</h2>
      @if(loading()){<p role="status">Cargando historial...</p>}
      @if(!loading() && entries().length===0){<p>Todavía no registraste tu descanso.</p>}
      @for(item of entries();track item.date){
        <p><strong>{{item.date}}</strong> — {{item.hours}} horas · calidad {{item.quality}}/5</p>
      }
    </section>
  `
})
export class SleepComponent implements OnInit {
  private readonly api=inject(DailyWellbeingApi);
  readonly entries=signal<SleepEntry[]>([]);
  readonly loading=signal(false);readonly saving=signal(false);
  readonly error=signal('');readonly message=signal('');
  readonly today=this.dateInLima();
  date=this.today;hours=8;quality=3;
  private dateInLima():string {
    const parts=new Intl.DateTimeFormat('en-US',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
    const part=(type:string)=>parts.find(p=>p.type===type)?.value??'';
    return `${part('year')}-${part('month')}-${part('day')}`;
  }
  ngOnInit(){this.load();}
  load(){this.loading.set(true);this.api.sleepHistory().pipe(finalize(()=>this.loading.set(false))).subscribe({
    next:items=>this.entries.set(items),error:()=>this.error.set('No se pudo cargar el historial.')
  });}
  save(){
    if(this.saving())return;
    if(!this.date || this.date>this.today || !Number.isFinite(this.hours) || this.hours<0 || this.hours>24
      || this.hours*4!==Math.trunc(this.hours*4) || !Number.isInteger(this.quality) || this.quality<1 || this.quality>5){
      this.error.set('Revisa la fecha, las horas (pasos de 15 minutos) y la calidad (1 a 5).');return;
    }
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.saveSleep(this.date,this.hours,this.quality).pipe(finalize(()=>this.saving.set(false))).subscribe({
      next:()=>{this.message.set('Registro de sueño guardado.');this.load();},
      error:()=>this.error.set('No pudimos guardar el descanso.')
    });
  }
}
