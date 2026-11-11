import {Component, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {ReminderApi, ReminderPreference} from '../../core/services/reminder-api.service';

@Component({
 selector:'app-reminders',standalone:true,imports:[FormsModule],
 template:`
 <header class="page-head"><div><span class="eyebrow">TUS HÁBITOS</span><h1>Recordatorios emocionales</h1>
 <p>Configura un aviso que aparecerá mientras tengas Mentaiko abierto. No se envían mensajes externos.</p></div></header>
 <section class="panel">
   @if(loading()){<p role="status">Cargando preferencias...</p>}
   @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
   @if(ready()){
     <form (ngSubmit)="save()">
       <label><input type="checkbox" name="enabled" [(ngModel)]="enabled"> Activar avisos dentro de Mentaiko</label>
       <label for="reminderTime">Hora de Lima</label>
       <input id="reminderTime" type="time" name="time" [(ngModel)]="time" required>
       <fieldset><legend>Días para recordar</legend>
         @for(day of weekdays;track day.number){
           <label><input type="checkbox" [name]="'day'+day.number" [ngModel]="selectedDays().includes(day.number)"
             (ngModelChange)="toggle(day.number,$event)"> {{day.label}}</label>
         }
       </fieldset>
       <button class="btn primary" type="submit" [disabled]="saving()">Guardar preferencias</button>
     </form>
     @if(due()){
       <div class="alert" role="status">
         <p>Es un buen momento para registrar cómo te sientes.</p>
         <a href="/app/checkins">Registrar emoción</a>
         <button type="button" (click)="acknowledge()">Marcar como visto</button>
       </div>
     }
     @if(message()){<p class="alert success" role="status">{{message()}}</p>}
   }
 </section>`
})
export class RemindersComponent implements OnInit {
 private readonly api=inject(ReminderApi);
 readonly weekdays=[
   {number:1,label:'Lunes'},{number:2,label:'Martes'},{number:3,label:'Miércoles'},
   {number:4,label:'Jueves'},{number:5,label:'Viernes'},{number:6,label:'Sábado'},
   {number:7,label:'Domingo'}
 ];
 readonly loading=signal(false);readonly saving=signal(false);
 readonly ready=signal(false);readonly due=signal(false);
 readonly error=signal('');readonly message=signal('');
 readonly selectedDays=signal<number[]>([1,2,3,4,5,6,7]);
 enabled=false;time='20:00';
 ngOnInit(){this.load();}
 load(){
   this.loading.set(true);this.error.set('');
   this.api.get().pipe(finalize(()=>this.loading.set(false))).subscribe({
     next:p=>this.apply(p),error:()=>this.error.set('No se pudieron cargar los recordatorios.')
   });
 }
 apply(p:ReminderPreference) {
   this.enabled=p.enabled;this.time=p.time.slice(0,5);
   this.selectedDays.set(p.days);this.due.set(p.due);this.ready.set(true);
 }
 toggle(day:number,checked:boolean){
   this.selectedDays.update(days=>checked?[...days,day].sort((a,b)=>a-b):days.filter(n=>n!==day));
 }
 save(){
   if(this.saving())return;
   if(!/^([01]\\d|2[0-3]):[0-5]\\d$/.test(this.time)||this.selectedDays().length===0){
     this.error.set('Selecciona una hora válida y por lo menos un día.');return;
   }
   this.error.set('');this.message.set('');this.saving.set(true);
   this.api.update(this.enabled,this.time,this.selectedDays()).pipe(finalize(()=>this.saving.set(false))).subscribe({
     next:p=>{this.apply(p);this.message.set('Preferencias guardadas.');},
     error:()=>this.error.set('No se pudieron guardar las preferencias.')
   });
 }
 acknowledge(){
   this.api.acknowledge().subscribe({
     next:p=>this.apply(p),
     error:()=>this.error.set('No se pudo confirmar el aviso.')
   });
 }
}
