import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { Emotion } from '../../../shared/models/models';

@Component({selector:'app-admin-emotions',standalone:true,imports:[ReactiveFormsModule],templateUrl: './admin-emotions.component.html',
  styleUrl: './admin-emotions.component.css'})
export class AdminEmotionsComponent implements OnInit{private readonly fb=inject(FormBuilder);readonly items=signal<Emotion[]>([]);readonly error=signal('');readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.maxLength(80)]]});constructor(private readonly api:ApiService){}ngOnInit():void{this.load();}load():void{this.api.emotions(false).subscribe({next:x=>this.items.set(x),error:()=>this.error.set('No se pudo cargar el catálogo.')});}create():void{if(this.form.invalid)return;this.api.createEmotion(this.form.getRawValue()).subscribe({next:()=>{this.form.reset();this.load();},error:e=>this.error.set(e.status===409?'La emoción ya existe.':'No se pudo crear.')});}toggle(e:Emotion):void{this.api.updateEmotion(e.id,{active:!e.active}).subscribe({next:()=>this.load(),error:()=>this.error.set('No se pudo cambiar el estado.')});}}
