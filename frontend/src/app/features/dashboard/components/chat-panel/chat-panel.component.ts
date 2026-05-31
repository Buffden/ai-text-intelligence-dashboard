import { Component, ElementRef, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ChatService } from '../../../../core/services/chat.service';
import { ChatMessage, ChatStreamEvent, ConversationSummary } from '../../../../core/models/chat.model';

const ACTIVE_CONVERSATION_KEY = 'chat_active_id';

@Component({
    selector: 'app-chat-panel',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './chat-panel.component.html',
    styleUrl: './chat-panel.component.scss',
})
export class ChatPanelComponent implements OnInit, OnDestroy {

    @ViewChild('messagesList') private messagesList!: ElementRef<HTMLDivElement>;

    conversations = signal<ConversationSummary[]>([]);
    messages = signal<ChatMessage[]>([]);
    loading = signal(false);
    error = signal<string | null>(null);
    sidebarCollapsed = signal(false);
    inputText = '';

    toggleSidebar(): void {
        this.sidebarCollapsed.update(v => !v);
    }

    private activeConversationId: string | null = null;
    private subscriptions = new Subscription();

    constructor(private chatService: ChatService) {}

    ngOnInit(): void {
        this.loadConversations();
    }

    private loadConversations(): void {
        const sub = this.chatService.getConversations().subscribe({
            next: (convs) => {
                this.conversations.set(convs);
                const savedId = localStorage.getItem(ACTIVE_CONVERSATION_KEY);
                // only restore if the conversation still exists on the server (not expired)
                if (savedId && convs.some(c => c.id === savedId)) {
                    this.selectConversation(savedId);
                } else if (savedId) {
                    localStorage.removeItem(ACTIVE_CONVERSATION_KEY);
                }
            },
            error: () => {} // sidebar failure is non-critical
        });
        this.subscriptions.add(sub);
    }

    selectConversation(id: string): void {
        if (id === this.activeConversationId) return;

        this.activeConversationId = id;
        localStorage.setItem(ACTIVE_CONVERSATION_KEY, id);
        this.messages.set([]);
        this.error.set(null);
        this.loading.set(true);

        const sub = this.chatService.getHistory(id).subscribe({
            next: (history) => {
                this.messages.set(history);
                this.loading.set(false);
                this.scrollToBottom();
            },
            error: () => {
                this.error.set('Failed to load conversation.');
                this.loading.set(false);
            }
        });
        this.subscriptions.add(sub);
    }

    newChat(): void {
        this.activeConversationId = null;
        localStorage.removeItem(ACTIVE_CONVERSATION_KEY);
        this.messages.set([]);
        this.error.set(null);
    }

    isActive(id: string): boolean {
        return this.activeConversationId === id;
    }

    send(): void {
        const text = this.inputText.trim();
        if (!text || this.loading()) return;

        this.error.set(null);
        this.inputText = '';
        this.loading.set(true);

        // add user message + empty assistant placeholder for streaming
        this.messages.update(msgs => [
            ...msgs,
            { role: 'user', content: text },
            { role: 'assistant', content: '' },
        ]);
        this.scrollToBottom();

        const sub = this.chatService.sendStream({
            conversationId: this.activeConversationId ?? undefined,
            message: text,
        }).subscribe({
            next: (event: ChatStreamEvent) => {
                if (event.type === 'conversation-id') {
                    this.activeConversationId = event.conversationId;
                    localStorage.setItem(ACTIVE_CONVERSATION_KEY, event.conversationId);
                } else if (event.type === 'token') {
                    this.messages.update(msgs => {
                        const updated = [...msgs];
                        const last = updated[updated.length - 1];
                        updated[updated.length - 1] = { ...last, content: last.content + event.token };
                        return updated;
                    });
                    this.scrollToBottom();
                } else if (event.type === 'done') {
                    this.loading.set(false);
                    this.refreshConversations();
                }
            },
            error: (err) => {
                // remove the empty assistant placeholder on failure
                this.messages.update(msgs => msgs.slice(0, -1));
                this.error.set(err.message ?? 'Something went wrong. Please try again.');
                this.loading.set(false);
            }
        });
        this.subscriptions.add(sub);
    }

    private refreshConversations(): void {
        const sub = this.chatService.getConversations().subscribe({
            next: (convs) => this.conversations.set(convs),
            error: () => {}
        });
        this.subscriptions.add(sub);
    }

    onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.send();
        }
    }

    private scrollToBottom(): void {
        setTimeout(() => {
            if (this.messagesList) {
                this.messagesList.nativeElement.scrollTop = this.messagesList.nativeElement.scrollHeight;
            }
        }, 0);
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }
}
