import {Component,OnInit,inject,signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {WeeklyGoal,WeeklyGoalApi} from '../../core/services/weekly-goal-api.service';

@Component({
 selector:'app-weekly-goal-card',
 standalone:true,
 imports:[RouterLink],
 templateUrl: './weekly-goal-card.component.html',
  styleUrl: './weekly-goal-card.component.css'
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
