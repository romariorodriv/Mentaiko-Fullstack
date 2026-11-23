import {Component,OnInit,inject,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {forkJoin} from 'rxjs';
import { CheckinsService } from '../../core/services/checkins.service';
import {Checkin} from '../../shared/models/models';
import {EmotionalFactorsApi, EmotionalFactor, FactorSummary, FactorEmotion} from '../../core/services/emotional-factors-api.service';

@Component({
 selector:'app-emotional-factors',standalone:true,imports:[FormsModule],
 templateUrl: './emotional-factors.component.html',
  styleUrl: './emotional-factors.component.css'
})
export class EmotionalFactorsComponent implements OnInit {
 private readonly checkinsApi=inject(CheckinsService);
 private readonly factors=inject(EmotionalFactorsApi);
 readonly catalog=signal<EmotionalFactor[]>([]);
 readonly checkins=signal<Checkin[]>([]);
 readonly summary=signal<FactorSummary[]>([]);
 readonly associations=signal<FactorEmotion[]>([]);
 readonly selectedId=signal<number|null>(null);
 readonly selectedFactors=signal<number[]>([]);
 readonly loading=signal(false);readonly analysisLoading=signal(false);
 readonly busy=signal(false);readonly error=signal('');readonly saved=signal('');
 ngOnInit(){this.load();this.loadSummary();}
 load(){
   this.loading.set(true);this.error.set('');
   forkJoin({catalog:this.factors.catalog(),history:this.checkinsApi.checkins({page:0,size:50})}).subscribe({
     next:result=>{this.catalog.set(result.catalog);this.checkins.set(result.history.content);this.loading.set(false);},
     error:()=>{this.loading.set(false);this.error.set('No pudimos cargar los registros emocionales.');}
   });
 }
 selectCheckin(id:number|null){
   this.selectedId.set(id);this.selectedFactors.set([]);this.saved.set('');
   if(id===null)return;
   this.factors.selection(id).subscribe({
     next:ids=>this.selectedFactors.set(ids),
     error:()=>this.error.set('No pudimos cargar los factores de este check-in.')
   });
 }
 toggle(id:number,checked:boolean){
   this.error.set('');this.saved.set('');
   const old=this.selectedFactors();
   if(checked && old.length>=5){this.error.set('Puedes seleccionar hasta cinco factores.');return;}
   this.selectedFactors.set(checked?[...old,id]:old.filter(x=>x!==id));
 }
 save(){
   const id=this.selectedId();if(id===null || this.busy())return;
   this.busy.set(true);this.saved.set('');this.error.set('');
   this.factors.save(id,this.selectedFactors()).subscribe({
     next:ids=>{this.selectedFactors.set(ids);this.busy.set(false);this.saved.set('Factores actualizados.');this.loadSummary();},
     error:()=>{this.busy.set(false);this.error.set('No fue posible guardar los factores.');}
   });
 }
 loadSummary(){
   const today=new Date();
   const end=this.localDay(today);
   const start=this.localDay(new Date(today.getTime()-29*86400000));
   this.analysisLoading.set(true);
   this.factors.analysis(start,end).subscribe({
     next:data=>{this.summary.set(data.factors);this.associations.set(data.associations??[]);this.analysisLoading.set(false);},
     error:()=>{this.summary.set([]);this.associations.set([]);this.analysisLoading.set(false);this.error.set('No pudimos cargar el análisis.');}
   });
 }
 private localDay(date:Date){
   return new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(date);
 }
}
