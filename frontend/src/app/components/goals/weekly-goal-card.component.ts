import {Component,OnInit,inject,signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {WeeklyGoal,WeeklyGoalApi} from '../../core/services/weekly-goal-api.service';

@Component({
 selector:'app-weekly-goal-card',
 standalone:true,
 imports:[RouterLink],
 template:`
 <article class="panel" aria-label="Progreso de meta semanal">
   <span class="eyebrow">TU META SEMANAL</span>
   <h2>Tu constancia</h2>
   @if(loading()){<p role="status">Consultando meta...</p>}
   @if(error()){<p>La meta semanal no está disponible en este momento.</p>}
   @if(goal();as current){
     @if(current.targetDays===null){
       <p>Aún no definiste una meta para esta semana.</p>
     }@else{
       <p><strong>{{current.completedDays}} de {{current.targetDays}} días</strong> con registro emocional.</p>
       <progress [value]="current.completedDays" [max]="current.targetDays">{{current.percentage}}%</progress>
       <p>{{current.percentage}}% del objetivo semanal</p>
     }
   }
   <a routerLink="/app/meta-semanal">Ver o ajustar meta</a>
 </article>`
})
export class WeeklyGoalCardComponent implements OnInit {
 private readonly api=inject(WeeklyGoalApi);
 readonly goal=signal<WeeklyGoal|null>(null);
 readonly loading=signal(true);
 readonly error=signal(false);
 ngOnInit(){
   this.api.get().subscribe({
     next:data=>{this.goal.set(data);this.loading.set(false);},
     error:()=>{this.error.set(true);this.loading.set(false);}
   });
 }
}
