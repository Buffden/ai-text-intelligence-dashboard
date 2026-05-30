import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-stream-result',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './stream-result.component.html',
    styleUrl: './stream-result.component.scss',
})
export class StreamResultComponent {
    @Input() text: string = '';
    @Input() streaming: boolean = false;
    @Input() error: string | null = null;
}
