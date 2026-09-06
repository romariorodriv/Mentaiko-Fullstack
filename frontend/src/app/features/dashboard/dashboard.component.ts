import { Component, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { DistributionItem, WeeklyPoint } from '../../shared/models/models';

@Component({selector:'app-dashboard',standalone:true,imports:[FormsModule,RouterLink,DecimalPipe],template:`
<header class="page-head"><div><span class="eyebrow">TU RESUMEN</span><h1>Hola, {{firstName()}}</h1><p>Una mirada amable a tu semana, sin juicios.</p></div><a class="btn primary" routerLink="/app/checkins">+ Nuevo check-in</a></header>
@if(loading()){<div class="loading-panel">Cargando tu resumen...</div>} @else if(error()){<div class="alert error">{{error()}} <button (click)="load()">Reintentar</button></div>} @else {
<section class="metric-grid"><article class="metric"><span>Registros esta semana</span><strong>{{total()}}</strong><small>Tu constancia construye perspectiva</small></article><article class="metric"><span>Intensidad promedio</span><strong>{{average()}}<i>/10</i></strong><small>Es información, no una calificación</small></article><article class="metric"><span>Emoción más frecuente</span><strong class="word">{{topEmotion()}}</strong><small>Basado en tus registros</small></article></section>
<section class="dashboard-grid"><article class="panel span-2"><div class="panel-head"><div><span class="eyebrow">EVOLUCIÓN SEMANAL</span><h2>Tu intensidad por día</h2></div><input type="week" [(ngModel)]="week" (change)="load()"></div><div class="bars">@for(p of weekly();track p.label){<div class="bar-item"><span class="bar-value">{{p.averageIntensity|number:'1.0-1'}}</span><div class="bar-track"><i [style.height.%]="p.averageIntensity*10"></i></div><small>{{p.label}}</small></div>}</div></article>
<article class="panel"><span class="eyebrow">DISTRIBUCIÓN</span><h2>Emociones</h2><div class="distribution">@for(item of emotions();track item.label){<div><span>{{item.label}}</span><b>{{item.percentage|number:'1.0-0'}}%</b><i><em [style.width.%]="item.percentage"></em></i></div>}</div></article></section>
<aside class="care-note"><span>◌</span><div><b>Una pausa también cuenta</b><p>Los patrones son una herramienta de reflexión, no un diagnóstico. Avanza a tu ritmo.</p></div><a routerLink="/app/actividades">Ver microactividades →</a></aside>}
`})
export class DashboardComponent implements OnInit{
 readonly loading=signal(true);readonly error=signal('');readonly weekly=signal<WeeklyPoint[]>([]);readonly emotions=signal<DistributionItem[]>([]);week=this.currentWeek();
 constructor(private readonly api:ApiService,private readonly auth:AuthService){}
 ngOnInit():void{this.load();}
 firstName():string{return this.auth.profile()?.name?.split(' ')[0]??'de nuevo';}
 load():void{this.loading.set(true);this.error.set('');forkJoin({w:this.api.weekly(this.week),d:this.api.distribution()}).subscribe({next:r=>{this.weekly.set(r.w);this.emotions.set(r.d.emotions);this.loading.set(false);},error:()=>{this.loading.set(false);this.error.set('No pudimos cargar el resumen desde la API.');}});}
 total():number{return this.weekly().reduce((s,x)=>s+x.count,0);} average():string{const a=this.weekly();return a.length?(a.reduce((s,x)=>s+x.averageIntensity,0)/a.length).toFixed(1):'0';}topEmotion():string{return this.emotions()[0]?.label??'Sin datos';}
 private currentWeek():string{const d=new Date();const start=new Date(d.getFullYear(),0,1);const w=Math.ceil((((d.getTime()-start.getTime())/86400000)+start.getDay()+1)/7);return `${d.getFullYear()}-W${String(w).padStart(2,'0')}`;}
}
