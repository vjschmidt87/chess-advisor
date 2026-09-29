import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'game', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('@features/auth/components/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('@features/auth/components/register/register.component').then(m => m.RegisterComponent) },
  { path: 'game', loadComponent: () => import('@features/game/components/game-page/game-page.component').then(m => m.GamePageComponent), canActivate: [authGuard] },
  { path: 'history', loadComponent: () => import('@features/history/components/history-page/history-page.component').then(m => m.HistoryPageComponent), canActivate: [authGuard] },
  { path: '**', redirectTo: 'game' }
];
