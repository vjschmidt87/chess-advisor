import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-notation-guide',
  standalone: true,
  templateUrl: './notation-guide.component.html',
  styleUrl: './notation-guide.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotationGuideComponent {
  isCollapsed = false;

  toggle(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}
