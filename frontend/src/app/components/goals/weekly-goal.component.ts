import {Component, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {WeeklyGoal, WeeklyGoalApi} from '../../core/services/weekly-goal-api.service';

@Component({
 selector:'app-weekly-goal', standalone:true, imports:[FormsModule],
 template:`
 <header class="page-head"><div>
   <span class="eyebrow">TUS HÁBITOS</span>
   <h1>Mi meta semanal</h1>
   <p>Elige cuántos días de esta semana deseas registrar tus emociones. Varios registros el mismo día cuentan como un solo día.</p>
 </div></header>
 <section class="panel">
   @if(loading()){<p role="status">Consultando tu meta...</p>}
   @if(error()){<p role="alert" class="alert error">{{error()}} <button type="button" (click)="load()">Reintentar</button></p>}
   @if(goal();as current){
     <h2>Semana del {{current.weekStart}}</h2>
     @if(current.targetDays===null){<p>Aún no has definido tu objetivo para esta semana.</p>}
     @else {
       <p><strong>{{current.completedDays}} de {{current.targetDays}} días</strong> completados.</p>
       <progress [value]="current.completedDays" [max]="current.targetDays"> {{current.percentage}}%</progress>
       <p>{{current.percentage}}% de cumplimiento · {{statusText(current.status)}}</p>
     }
     <form (ngSubmit)="save()">
       <label for="targetDays">Mi objetivo de registros en días distintos</label>
       <select id="targetDays" name="targetDays" [(ngModel)]="targetDays" required>
         @for(day of days;track day){<option [ngValue]="day">{{day}} día{{day===1?'':'s'}} por semana</option>}
       </select>
       <button class="btn primary" type="submit" [disabled]="saving()">Guardar mi meta</button>
     </form>
   }
   @if(message()){<p role="status" class="alert success">{{message()}}</p>}
 </section>`
})
export class WeeklyGoalComponent implements OnInit {
 private readonly api=inject(WeeklyGoalApi);
 readonly days=[1,2,3,4,5,6,7];
 readonly loading=signal(false);
 readonly saving=signal(false);
 readonly error=signal('');
 readonly message=signal('');
 readonly goal=signal<WeeklyGoal|null>(null);
 targetDays=3;
 ngOnInit(){this.load();}
 statusText(status:WeeklyGoal['status']):string {
   return ({SIN_META:'Sin meta',PENDIENTE:'Aún no comienzas',EN_PROGRESO:'En progreso',CUMPLIDA:'¡Meta alcanzada!'})[status];
 }
 load(){
   this.loading.set(true);this.error.set('');
   this.api.get().pipe(finalize(()=>this.loading.set(false))).subscribe({
     next:goal=>{this.goal.set(goal);this.targetDays=goal.targetDays??3;},
     error:()=>this.error.set('No fue posible consultar tu meta semanal.')
   });
 }
 save(){
   if(this.saving() || !Number.isInteger(this.targetDays) || this.targetDays<1 || this.targetDays>7)return;
   this.saving.set(true);this.error.set('');this.message.set('');
   this.api.save(this.targetDays).pipe(finalize(()=>this.saving.set(false))).subscribe({
     next:goal=>{this.goal.set(goal);this.message.set('Meta semanal actualizada.');},
     error:()=>this.error.set('No pudimos guardar tu meta semanal.')
   });
 }
}
