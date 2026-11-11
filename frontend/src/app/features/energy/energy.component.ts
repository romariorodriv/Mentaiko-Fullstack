import {Component, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {DailyWellbeingApi, EnergyEntry} from '../../core/services/daily-wellbeing.service';

@Component({
  selector:'app-energy',
  standalone:true,
  imports:[FormsModule],
  template:`
    <header class="page-head"><div><span class="eyebrow">CÓMO TE SIENTES</span><h1>Mi energía diaria</h1>
      <p>Registra tu nivel de energía de 1 (muy baja) a 5 (muy alta).</p></div></header>
    <section class="panel">
      <form (ngSubmit)="save()">
        <label>Fecha <input type="date" name="date" [(ngModel)]="date" [max]="today" required></label>
        <label>Nivel de energía
          <select name="level" [(ngModel)]="level" required>
            <option [ngValue]="1">1 — Muy baja</option><option [ngValue]="2">2 — Baja</option>
            <option [ngValue]="3">3 — Media</option><option [ngValue]="4">4 — Alta</option>
            <option [ngValue]="5">5 — Muy alta</option>
          </select>
        </label>
        <button class="btn primary" type="submit" [disabled]="saving()">Guardar nivel</button>
        <p>Un registro por día. Guardar de nuevo reemplaza el valor de esa fecha.</p>
      </form>
      @if(message()){<p class="alert success" role="status">{{message()}}</p>}
      @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
    </section>
    <section class="panel"><h2>Últimos 30 días</h2>
      @if(loading()){<p role="status">Cargando historial...</p>}
      @if(!loading() && entries().length===0){<p>Todavía no registraste tu energía.</p>}
      @for(item of entries();track item.date){
        <p><strong>{{item.date}}</strong> — energía {{item.level}}/5</p>
      }
    </section>
  `
})
export class EnergyComponent implements OnInit {
  private readonly api=inject(DailyWellbeingApi);
  readonly entries=signal<EnergyEntry[]>([]);
  readonly loading=signal(false);readonly saving=signal(false);
  readonly error=signal('');readonly message=signal('');
  readonly today=this.dateInLima();
  date=this.today;level=3;
  private dateInLima():string {
    const parts=new Intl.DateTimeFormat('en-US',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
    const part=(type:string)=>parts.find(p=>p.type===type)?.value??'';
    return `${part('year')}-${part('month')}-${part('day')}`;
  }
  ngOnInit(){this.load();}
  load(){this.loading.set(true);this.api.energyHistory().pipe(finalize(()=>this.loading.set(false))).subscribe({
    next:items=>this.entries.set(items),error:()=>this.error.set('No se pudo cargar el historial.')
  });}
  save(){
    if(this.saving())return;
    if(!this.date || this.date>this.today || !Number.isInteger(this.level) || this.level<1 || this.level>5){
      this.error.set('Elige una fecha válida y un nivel entre 1 y 5.');return;
    }
    this.saving.set(true);this.error.set('');this.message.set('');
    this.api.saveEnergy(this.date,this.level).pipe(finalize(()=>this.saving.set(false))).subscribe({
      next:()=>{this.message.set('Nivel de energía guardado.');this.load();},
      error:()=>this.error.set('No pudimos guardar tu nivel de energía.')
    });
  }
}
