import {Component,OnInit,inject,signal} from '@angular/core';
import {ActivityPreferencesApi} from '../../core/services/activity-preferences-api.service';

@Component({
 selector:'app-activity-preferences',standalone:true,
 templateUrl: './activity-preferences.component.html',
  styleUrl: './activity-preferences.component.css'
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
