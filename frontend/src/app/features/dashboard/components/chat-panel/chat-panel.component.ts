import { Component, ElementRef, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ChatService } from '../../../../core/services/chat.service';
import { ChatMessage } from '../../../../core/models/chat.model';

const CONVERSATION_ID_KEY = 'chat_conversation_id';

@Component({
    selector: 'app-chat-panel',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './chat-panel.component.html',
    styleUrl: './chat-panel.component.scss',
})
export class ChatPanelComponent implements OnInit, OnDestroy {

    @ViewChild('messagesList') private messagesList!: ElementRef<HTMLDivElement>;

    messages = signal<ChatMessage[]>([]);
    loading = signal(false);
    error = signal<string | null>(null);
    inputText = '';

    private conversationId: string | null = null;
    private subscription: Subscription | null = null;

    constructor(private chatService: ChatService) { }

    ngOnInit(): void {
        const savedId = localStorage.getItem(CONVERSATION_ID_KEY);
        if (savedId) {
            this.conversationId = savedId;
            this.loading.set(true);
            this.subscription = this.chatService.getHistory(savedId).subscribe({
                next: (history) => {
                    this.messages.set(history);
                    this.loading.set(false);
                    this.scrollToBottom();
                },
                error: () => {
                    // conversation expired or not found, start fresh
                    localStorage.removeItem(CONVERSATION_ID_KEY);
                    this.conversationId = null;
                    this.loading.set(false);
                },
            });
        }
    }

    send(): void {
        const text = this.inputText.trim();
        if (!text || this.loading()) return;

        this.error.set(null);
        this.messages.update(msgs => [...msgs, { role: 'user', content: text }]);
        this.inputText = '';
        this.loading.set(true);
        this.scrollToBottom();

        this.subscription?.unsubscribe();
        this.subscription = this.chatService.send({
            conversationId: this.conversationId ?? undefined,
            message: text,
        }).subscribe({
            next: (reply) => {
                this.conversationId = reply.conversationId;
                localStorage.setItem(CONVERSATION_ID_KEY, reply.conversationId);
                this.messages.update(msgs => [...msgs, { role: 'assistant', content: reply.reply }]);
                this.loading.set(false);
                this.scrollToBottom();
            },
            error: (err) => {
                this.error.set(err.message ?? 'Something went wrong. Please try again.');
                this.loading.set(false);
            },
        });
    }

    onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.send();
        }
    }

    clearConversation(): void {
        this.subscription?.unsubscribe();
        this.conversationId = null;
        localStorage.removeItem(CONVERSATION_ID_KEY);
        this.messages.set([]);
        this.error.set(null);
        this.loading.set(false);
    }

    private scrollToBottom(): void {
        setTimeout(() => {
            if (this.messagesList) {
                this.messagesList.nativeElement.scrollTop = this.messagesList.nativeElement.scrollHeight;
            }
        }, 0);
    }

    ngOnDestroy(): void {
        this.subscription?.unsubscribe();
    }
}
