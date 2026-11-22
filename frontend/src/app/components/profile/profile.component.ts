import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';

@Component({selector:'app-profile',standalone:true,imports:[ReactiveFormsModule],template:`
<header class="page-head"><div><span class="eyebrow">CUENTA</span><h1>Mi perfil</h1><p>Manten actualizados los datos que usamos para personalizar tu experiencia.</p></div></header>
@if(loading()){<div class="loading-panel">Cargando perfil...</div>} @else {
<section class="profile-layout"><aside class="profile-card"><span class="profile-avatar">{{initials()}}</span><h2>{{auth.profile()?.name}}</h2><p>{{auth.profile()?.email}}</p><span class="role-chip">{{auth.profile()?.role==='ADMIN'?'Administrador':'Estudiante'}}</span><small>Miembro desde {{auth.profile()?.createdAt||'tu registro'}}</small></aside><form class="panel profile-form" [formGroup]="form" (ngSubmit)="save()"><div class="panel-head"><div><span class="eyebrow">INFORMACION PERSONAL</span><h2>Datos del perfil</h2></div></div><label>Nombre completo<input formControlName="name"></label><label>Correo electronico<input [value]="auth.profile()?.email" disabled><small>El correo no puede modificarse desde el perfil.</small></label><div class="form-grid"><label>Universidad<input formControlName="university"></label><label>Carrera<input formControlName="career"></label></div>@if(message()){<div class="alert success">{{message()}}</div>}@if(error()){<div class="alert error">{{error()}}</div>}<button class="btn primary" [disabled]="form.invalid||saving()">{{saving()?'Guardando...':'Guardar cambios'}}</button></form></section>}
`})
export class ProfileComponent implements OnInit{
 private readonly fb=inject(FormBuilder);readonly loading=signal(true);readonly saving=signal(false);readonly message=signal('');readonly error=signal('');readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.maxLength(100)]],university:['',[Validators.required,Validators.maxLength(150)]],career:['',[Validators.required,Validators.maxLength(150)]]});
 constructor(public readonly auth:AuthService){}
 ngOnInit():void{const apply=()=>{const p=this.auth.profile();if(p)this.form.patchValue({name:p.name,university:p.university,career:p.career});this.loading.set(false);};if(this.auth.profile())apply();else this.auth.loadProfile().subscribe({next:apply,error:()=>{this.loading.set(false);this.error.set('No se pudo cargar el perfil.');}});}
 initials():string{return(this.auth.profile()?.name??'M').split(' ').slice(0,2).map(x=>x[0]).join('').toUpperCase();}
 save():void{if(this.form.invalid){this.form.markAllAsTouched();return;}this.saving.set(true);this.message.set('');this.error.set('');this.auth.updateProfile(this.form.getRawValue()).subscribe({next:()=>{this.saving.set(false);this.message.set('Perfil actualizado correctamente.');},error:e=>{this.saving.set(false);this.error.set(e.error?.message??'No se pudieron guardar los cambios.');}});}
}
