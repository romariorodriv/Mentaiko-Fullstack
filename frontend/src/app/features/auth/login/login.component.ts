import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({selector:'app-login', standalone:true, imports:[ReactiveFormsModule,RouterLink], template:`
<main class="auth-page">
  <section class="auth-story"><a class="brand brand-light"><img class="brand-logo" src="/branding/mentaiko-logo.svg" alt="Mentaiko"></a><div><span class="eyebrow light">BIENESTAR UNIVERSITARIO</span><h1>Entender cómo te sientes también es avanzar.</h1><p>Registra tus emociones, reconoce patrones y encuentra pequeñas acciones para cuidar de ti.</p></div><small>Bienestar emocional no clínico. No reemplaza atención profesional.</small></section>
  <section class="auth-form-wrap"><form class="auth-form" [formGroup]="form" (ngSubmit)="submit()"><span class="eyebrow">BIENVENIDO DE VUELTA</span><h2>Inicia sesión</h2><p>Continúa desde donde lo dejaste.</p>
    <label>Correo electrónico<input type="email" formControlName="email" placeholder="tu@universidad.edu.pe"></label>
    <label>Contraseña<input type="password" formControlName="password" placeholder="Tu contraseña"></label>
    @if(error()){<div class="alert error">{{error()}}</div>}
    <button class="btn primary full" [disabled]="form.invalid || loading()">{{loading()?'Ingresando...':'Ingresar a Mentaiko'}}</button>
    <p class="auth-switch">¿Aún no tienes cuenta? <a routerLink="/registro">Crear cuenta</a></p>
  </form></section>
</main>`})
export class LoginComponent {
  private readonly fb=inject(FormBuilder);
  readonly loading=signal(false); readonly error=signal('');
  readonly form=this.fb.nonNullable.group({email:['',[Validators.required,Validators.email]],password:['',Validators.required]});
  constructor(private readonly auth:AuthService,private readonly router:Router){}
  submit():void{if(this.form.invalid)return;this.loading.set(true);this.error.set('');this.auth.login(this.form.getRawValue().email,this.form.getRawValue().password).subscribe({next:()=>void this.router.navigate(['/app/dashboard']),error:e=>{this.loading.set(false);this.error.set(e.status===401?'El correo o la contraseña no son correctos.':'No pudimos conectar con el servidor. Intenta nuevamente.');}});}
}
