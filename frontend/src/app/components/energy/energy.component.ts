import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {DailyWellbeingApi, EnergyEntry} from '../../core/services/daily-wellbeing.service';

@Component({
  selector:'app-energy',
  standalone:true,
  imports:[FormsModule],
  templateUrl: './energy.component.html',
  styleUrl: './energy.component.css'
})
export class EnergyComponent implements OnInit {
  private readonly api=inject(DailyWellbeingApi);
  readonly entries=signal<EnergyEntry[]>([]);
  readonly averageEnergy=computed(()=>{
    const items=this.entries();
    return items.length?(items.reduce((sum,item)=>sum+item.level,0)/items.length).toFixed(1):'0';
  });
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
