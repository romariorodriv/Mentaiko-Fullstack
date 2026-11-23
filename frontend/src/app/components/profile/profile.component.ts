import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';

@Component({selector:'app-profile',standalone:true,imports:[ReactiveFormsModule],templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'})
export class ProfileComponent implements OnInit{
 private readonly fb=inject(FormBuilder);readonly loading=signal(true);readonly saving=signal(false);readonly message=signal('');readonly error=signal('');readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.maxLength(100)]],university:['',[Validators.required,Validators.maxLength(150)]],career:['',[Validators.required,Validators.maxLength(150)]]});
 constructor(public readonly auth:AuthService){}
 ngOnInit():void{const apply=()=>{const p=this.auth.profile();if(p)this.form.patchValue({name:p.name,university:p.university,career:p.career});this.loading.set(false);};if(this.auth.profile())apply();else this.auth.loadProfile().subscribe({next:apply,error:()=>{this.loading.set(false);this.error.set('No se pudo cargar el perfil.');}});}
 initials():string{return(this.auth.profile()?.name??'M').split(' ').slice(0,2).map(x=>x[0]).join('').toUpperCase();}
 save():void{if(this.form.invalid){this.form.markAllAsTouched();return;}this.saving.set(true);this.message.set('');this.error.set('');this.auth.updateProfile(this.form.getRawValue()).subscribe({next:()=>{this.saving.set(false);this.message.set('Perfil actualizado correctamente.');},error:e=>{this.saving.set(false);this.error.set(e.error?.message??'No se pudieron guardar los cambios.');}});}
}
