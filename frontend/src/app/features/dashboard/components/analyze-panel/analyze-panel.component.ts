import { Component, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { LlmStreamService } from '../../../../core/services/llm-stream.service';
import { StreamResultComponent } from '../stream-result/stream-result.component';
import { StreamState } from '../../../../core/models/analysis.model';
import { ANALYZE_PANEL_CONFIG } from './analyze-panel.config';

@Component({
    selector: 'app-analyze-panel',
    standalone: true,
    imports: [CommonModule, FormsModule, StreamResultComponent],
    templateUrl: './analyze-panel.component.html',
    styleUrl: './analyze-panel.component.scss',
})
export class AnalyzePanelComponent implements OnDestroy {

    protected readonly config = ANALYZE_PANEL_CONFIG;

    inputText: string = '';
    state = signal<StreamState>({ streaming: false, text: '', error: null });

    private subscription: Subscription | null = null;

    constructor(private llmStreamService: LlmStreamService) {}

    analyze(): void {
        if (!this.inputText.trim() || this.state().streaming) return;

        this.state.set({ streaming: true, text: '', error: null });
        this.subscription?.unsubscribe();

        this.subscription = this.llmStreamService.analyzeStream(this.inputText).subscribe({
            next: (token) => {
                this.state.update(s => ({ ...s, text: s.text + token }));
            },
            error: (err) => {
                this.state.update(s => ({ ...s, streaming: false, error: err.message }));
            },
            complete: () => {
                this.state.update(s => ({ ...s, streaming: false }));
            },
        });
    }

    ngOnDestroy(): void {
        this.subscription?.unsubscribe();
    }
}
