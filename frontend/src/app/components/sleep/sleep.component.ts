import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {DailyWellbeingApi, SleepEntry} from '../../core/services/daily-wellbeing.service';

@Component({
  selector:'app-sleep',
  standalone:true,
  imports:[FormsModule],
  templateUrl: './sleep.component.html',
  styleUrl: './sleep.component.css'
})
export class SleepComponent implements OnInit {
  private readonly api=inject(DailyWellbeingApi);
  readonly entries=signal<SleepEntry[]>([]);
  readonly averageHours=computed(()=>this.average(item=>item.hours));
  readonly averageQuality=computed(()=>this.average(item=>item.quality));
  private average(get:(item:SleepEntry)=>number):string{
    const values=this.entries();
    return values.length ? (values.reduce((sum,item)=>sum+get(item),0)/values.length).toFixed(1) : '0';
  }
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
