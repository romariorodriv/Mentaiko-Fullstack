import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({selector:'app-register',standalone:true,imports:[ReactiveFormsModule,RouterLink],template:`
<main class="auth-page"><section class="auth-story"><a class="brand brand-light"><img class="brand-logo" src="/branding/mentaiko-logo.svg" alt="Mentaiko"></a><div><span class="eyebrow light">EMPIEZA A TU RITMO</span><h1>Un espacio privado para escucharte.</h1><p>Tus registros te pertenecen. La experiencia esta disenada para acompanarte, no para diagnosticarte.</p></div><small>Tu informacion se utiliza unicamente para brindarte tu experiencia personal.</small></section>
<section class="auth-form-wrap"><form class="auth-form wide" [formGroup]="form" (ngSubmit)="submit()"><span class="eyebrow">CREA TU CUENTA</span><h2>Comencemos</h2><div class="form-grid">
<label class="span-2">Nombre completo<input formControlName="name" placeholder="Como te llamas?">@if(showError('name')){<small>Ingresa al menos 2 caracteres.</small>}</label>
<label>Fecha de nacimiento<input type="date" formControlName="birthDate" [max]="maxBirthDate">@if(showError('birthDate')){<small>Selecciona una fecha pasada.</small>}</label>
<label>Universidad<input formControlName="university" placeholder="UPC">@if(showError('university')){<small>La universidad es obligatoria.</small>}</label>
<label>Carrera<input formControlName="career" placeholder="Ingenieria de Software">@if(showError('career')){<small>La carrera es obligatoria.</small>}</label>
<label class="span-2">Correo electronico<input type="email" formControlName="email" placeholder="tu@universidad.edu.pe">@if(showError('email')){<small>Ingresa un correo valido.</small>}</label>
<label>Contrasena<input type="password" formControlName="password" placeholder="Minimo 8 caracteres">@if(showError('password')){<small>Usa entre 8 y 72 caracteres.</small>}</label>
<label>Confirmar contrasena<input type="password" formControlName="confirmPassword" placeholder="Repitela">@if(showError('confirmPassword')){<small>Confirma tu contrasena.</small>}</label></div>
@if(error()){<div class="alert error">{{error()}}</div>}<button class="btn primary full" [disabled]="form.invalid||loading()">{{loading()?'Creando cuenta...':'Crear mi cuenta'}}</button><p class="auth-switch">Ya tienes cuenta? <a routerLink="/login">Iniciar sesion</a></p></form></section></main>`})
export class RegisterComponent{
 private readonly fb=inject(FormBuilder);
 readonly loading=signal(false);readonly error=signal('');readonly maxBirthDate=this.yesterday();readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.minLength(2),Validators.maxLength(100)]],birthDate:['',[Validators.required,this.pastDateValidator.bind(this)]],university:['',[Validators.required,Validators.maxLength(150)]],career:['',[Validators.required,Validators.maxLength(150)]],email:['',[Validators.required,Validators.email,Validators.maxLength(150)]],password:['',[Validators.required,Validators.minLength(8),Validators.maxLength(72)]],confirmPassword:['',Validators.required]});
 constructor(private readonly auth:AuthService,private readonly router:Router){}
 submit():void{const v=this.form.getRawValue();if(this.form.invalid){this.form.markAllAsTouched();return;}if(v.password!==v.confirmPassword){this.error.set('Las contrasenas no coinciden.');return;}this.loading.set(true);this.error.set('');const{name,email,password,birthDate,university,career}=v;this.auth.register({name,email,password,birthDate,university,career}).subscribe({next:()=>void this.router.navigate(['/login']),error:e=>{this.loading.set(false);this.error.set(e.status===409?'Ese correo ya esta registrado.':e.error?.message??'Revisa los datos o la conexion con el servidor.');}});}
 showError(name:string):boolean{const control=this.form.get(name);return !!control&&(control.touched||control.dirty)&&control.invalid;}
 private yesterday():string{const d=new Date();d.setDate(d.getDate()-1);return d.toISOString().slice(0,10);}
 private pastDateValidator(control:AbstractControl){if(!control.value)return null;return control.value<=this.yesterday()?null:{pastDate:true};}
}
