import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyzePanelComponent } from './components/analyze-panel/analyze-panel.component';
import { ClassifyPanelComponent } from './components/classify-panel/classify-panel.component';
import { ChatPanelComponent } from './components/chat-panel/chat-panel.component';

type Tab = 'analyze' | 'classify' | 'chat';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, AnalyzePanelComponent, ClassifyPanelComponent, ChatPanelComponent],
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
    activeTab = signal<Tab>('analyze');

    setTab(tab: Tab): void {
        this.activeTab.set(tab);
    }
}
