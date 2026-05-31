import { Component, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { LlmStreamService } from '../../../../core/services/llm-stream.service';
import { ClassifyResult } from '../../../../core/models/analysis.model';
import { CLASSIFY_PANEL_CONFIG } from './classify-panel.config';

@Component({
    selector: 'app-classify-panel',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './classify-panel.component.html',
    styleUrl: './classify-panel.component.scss',
})
export class ClassifyPanelComponent implements OnDestroy {

    protected readonly config = CLASSIFY_PANEL_CONFIG;

    inputText = '';
    result = signal<ClassifyResult | null>(null);
    loading = signal(false);
    error = signal<string | null>(null);

    private subscription: Subscription | null = null;

    constructor(private llmStreamService: LlmStreamService) {}

    classify(): void {
        if (!this.inputText.trim() || this.loading()) return;

        this.result.set(null);
        this.error.set(null);
        this.loading.set(true);
        this.subscription?.unsubscribe();

        this.subscription = this.llmStreamService.classify(this.inputText).subscribe({
            next: (res) => {
                this.result.set(res);
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set(err.message ?? 'Classification failed. Please try again.');
                this.loading.set(false);
            }
        });
    }

    get confidencePercent(): number {
        return Math.round((this.result()?.confidence ?? 0) * 100);
    }

    ngOnDestroy(): void {
        this.subscription?.unsubscribe();
    }
}
