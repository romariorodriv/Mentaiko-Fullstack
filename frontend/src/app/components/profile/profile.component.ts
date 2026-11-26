import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({selector:'app-profile',standalone:true,imports:[ReactiveFormsModule,RouterLink],templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'})
export class ProfileComponent implements OnInit{
 private readonly fb=inject(FormBuilder);readonly deleting=signal(false);readonly confirmingDeletion=signal(false);readonly deletionPassword=signal('');readonly loading=signal(true);readonly saving=signal(false);readonly message=signal('');readonly error=signal('');readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.maxLength(100)]],university:['',[Validators.required,Validators.maxLength(150)]],career:['',[Validators.required,Validators.maxLength(150)]]});
 constructor(public readonly auth:AuthService){}
 deleteAccount():void{if(this.deleting()||!this.deletionPassword())return;this.deleting.set(true);this.error.set('');this.auth.deleteAccount(this.deletionPassword()).subscribe({next:()=>{this.deleting.set(false);this.deletionPassword.set('');},error:()=>{this.deleting.set(false);this.error.set('No se pudo eliminar la cuenta. Verifica tu contraseña e inténtalo nuevamente.');}});}
 ngOnInit():void{const apply=()=>{const p=this.auth.profile();if(p)this.form.patchValue({name:p.name,university:p.university,career:p.career});this.loading.set(false);};if(this.auth.profile())apply();else this.auth.loadProfile().subscribe({next:apply,error:()=>{this.loading.set(false);this.error.set('No se pudo cargar el perfil.');}});}
 initials():string{return(this.auth.profile()?.name??'M').split(' ').slice(0,2).map(x=>x[0]).join('').toUpperCase();}
 save():void{if(this.form.invalid){this.form.markAllAsTouched();return;}this.saving.set(true);this.message.set('');this.error.set('');this.auth.updateProfile(this.form.getRawValue()).subscribe({next:()=>{this.saving.set(false);this.message.set('Perfil actualizado correctamente.');},error:e=>{this.saving.set(false);this.error.set(e.error?.message??'No se pudieron guardar los cambios.');}});}
}
