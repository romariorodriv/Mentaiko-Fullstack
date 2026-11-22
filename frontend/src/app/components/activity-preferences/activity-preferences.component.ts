import {Component,OnInit,inject,signal} from '@angular/core';
import {ActivityPreferencesApi} from '../../core/services/activity-preferences-api.service';

@Component({
 selector:'app-activity-preferences',standalone:true,
 template:`
 <header class="page-head"><div><span class="eyebrow">PERSONALIZACIÓN</span>
 <h1>Mis actividades preferidas</h1>
 <p>Elige qué tipos de microactividades prefieres. Seguirás recibiendo recomendaciones cuando no exista una coincidencia.</p>
 </div></header>
 <section class="panel">
 @if(loading()){<p role="status">Cargando preferencias...</p>}
 @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
 @if(ready()){
   <fieldset><legend>Tipos de actividades</legend>
   @for(type of available();track type){
     <label><input type="checkbox" [checked]="selected().includes(type)"
       (change)="toggle(type,$any($event.target).checked)"> {{label(type)}}</label>
   }
   </fieldset>
   <p>Puedes dejar todos los tipos sin seleccionar para recibir el comportamiento habitual.</p>
   <button type="button" class="btn primary" [disabled]="saving()" (click)="save()">Guardar preferencias</button>
   @if(message()){<p class="alert success" role="status">{{message()}}</p>}
 }
 </section>`
})
export class ActivityPreferencesComponent implements OnInit {
 private readonly api=inject(ActivityPreferencesApi);
 readonly available=signal<string[]>([]);
 readonly selected=signal<string[]>([]);
 readonly ready=signal(false);readonly loading=signal(false);readonly saving=signal(false);
 readonly error=signal('');readonly message=signal('');
 private readonly names:Record<string,string>={
   GENERAL:'General',RESPIRACION:'Respiración',MOVIMIENTO:'Movimiento',
   RELAJACION:'Relajación',ESCRITURA:'Escritura',ATENCION_PLENA:'Atención plena'
 };
 ngOnInit(){this.load();}
 label(type:string){return this.names[type]??type;}
 load(){
   this.loading.set(true);this.error.set('');
   this.api.get().subscribe({
     next:data=>{this.available.set(data.available);this.selected.set(data.selected);this.ready.set(true);this.loading.set(false);},
     error:()=>{this.error.set('No se pudieron cargar tus preferencias.');this.loading.set(false);}
   });
 }
 toggle(type:string,checked:boolean){
   this.selected.update(current=>checked?[...current,type]:current.filter(t=>t!==type));
   this.message.set('');
 }
 save(){
   if(this.saving())return;
   this.saving.set(true);this.error.set('');this.message.set('');
   this.api.save(this.selected()).subscribe({
     next:data=>{this.selected.set(data.selected);this.saving.set(false);this.message.set('Preferencias actualizadas.');},
     error:()=>{this.saving.set(false);this.error.set('No se pudieron guardar tus preferencias.');}
   });
 }
}
