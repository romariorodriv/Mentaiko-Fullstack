import {Component,OnInit,inject,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {forkJoin} from 'rxjs';
import {ApiService} from '../../core/services/api.service';
import {Checkin} from '../../shared/models/models';
import {EmotionalFactorsApi, EmotionalFactor, FactorSummary} from '../../core/services/emotional-factors-api.service';

@Component({
 selector:'app-emotional-factors',standalone:true,imports:[FormsModule],
 template:`
 <header class="page-head"><div><span class="eyebrow">AUTOCONOCIMIENTO</span>
 <h1>Factores de mis emociones</h1>
 <p>Identifica circunstancias que coinciden con tus emociones. Estas asociaciones no indican causas ni son un diagnóstico.</p></div></header>
 <section class="panel">
   <h2>Asociar factores a un check-in</h2>
   @if(loading()){<p role="status">Cargando registros...</p>}
   @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
   @if(!loading()){
     <label>Registro emocional
       <select name="entry" [ngModel]="selectedId()" (ngModelChange)="selectCheckin($event)">
       <option [ngValue]="null">Selecciona un check-in</option>
       @for(item of checkins();track item.id){
         <option [ngValue]="item.id">{{item.createdAt.slice(0,10)}} · {{item.emotion.name}} · {{item.context}}</option>
       }
       </select>
     </label>
     @if(selectedId()!==null){
       <fieldset><legend>Elige hasta cinco factores</legend>
       @for(factor of catalog();track factor.id){
         <label><input type="checkbox" [checked]="selectedFactors().includes(factor.id)"
           (change)="toggle(factor.id,$any($event.target).checked)"> {{factor.label}}</label>
       }
       </fieldset>
       <button class="btn primary" type="button" (click)="save()" [disabled]="busy()">Guardar factores</button>
       @if(saved()){<p role="status" class="alert success">{{saved()}}</p>}
     }
     @if(checkins().length===0){<p>No hay check-ins recientes. Registra primero una emoción.</p>}
   }
 </section>
 <section class="panel">
   <h2>Factores frecuentes — últimos 30 días</h2>
   @if(analysisLoading()){<p role="status">Analizando registros...</p>}
   @for(item of summary();track item.id){
     <p>{{item.label}} — {{item.count}} registro{{item.count===1?'':'s'}}</p>
   } @empty {<p>Sin factores registrados en este período.</p>}
   <small>Resumen descriptivo de los factores seleccionados, sin analizar notas personales.</small>
 </section>`
})
export class EmotionalFactorsComponent implements OnInit {
 private readonly api=inject(ApiService);
 private readonly factors=inject(EmotionalFactorsApi);
 readonly catalog=signal<EmotionalFactor[]>([]);
 readonly checkins=signal<Checkin[]>([]);
 readonly summary=signal<FactorSummary[]>([]);
 readonly selectedId=signal<number|null>(null);
 readonly selectedFactors=signal<number[]>([]);
 readonly loading=signal(false);readonly analysisLoading=signal(false);
 readonly busy=signal(false);readonly error=signal('');readonly saved=signal('');
 ngOnInit(){this.load();this.loadSummary();}
 load(){
   this.loading.set(true);this.error.set('');
   forkJoin({catalog:this.factors.catalog(),history:this.api.checkins({page:0,size:50})}).subscribe({
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
     next:data=>{this.summary.set(data.factors);this.analysisLoading.set(false);},
     error:()=>{this.summary.set([]);this.analysisLoading.set(false);this.error.set('No pudimos cargar el análisis.');}
   });
 }
 private localDay(date:Date){
   return new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(date);
 }
}
