import {Component, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {WeeklyGoal, WeeklyGoalApi} from '../../core/services/weekly-goal-api.service';

@Component({
 selector:'app-weekly-goal', standalone:true, imports:[FormsModule],
 templateUrl: './weekly-goal.component.html',
  styleUrl: './weekly-goal.component.css'
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
