import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class LlmStreamService {

    analyzeStream(text: string): Observable<string> {
        return new Observable<string>((observer) => {
            const controller = new AbortController();
            const url = `http://localhost:8080/api/analyze/stream?text=${encodeURIComponent(text)}`;

            fetch(url, {
                method: 'GET',
                headers: { 'Accept': 'text/event-stream' },
                signal: controller.signal,
            })
                .then(async (response) => {
                    if (!response.ok) {
                        throw new Error(`HTTP error: ${response.status}`);
                    }
                    if (!response.body) {
                        throw new Error('ReadableStream not supported.');
                    }

                    const reader = response.body
                        .pipeThrough(new TextDecoderStream())
                        .getReader();

                    let buffer = '';

                    while (true) {
                        const { value, done } = await reader.read();
                        if (done) break;

                        buffer += value;
                        const events = buffer.split('\n\n');
                        buffer = events.pop() ?? '';

                        for (const event of events) {
                            const lines = event.split('\n');
                            const eventName = lines.find(l => l.startsWith('event:'))?.slice('event:'.length).trim();
                            const data = lines.find(l => l.startsWith('data:'))?.slice('data:'.length);

                            if (eventName === 'error') {
                                observer.error(new Error(data ?? 'Stream error'));
                                return;
                            }

                            if (data === '[DONE]') {
                                observer.complete();
                                return;
                            }

                            if (data !== undefined) {
                                observer.next(data);
                            }
                        }
                    }

                    observer.complete();
                })
                .catch((error) => {
                    if (error.name !== 'AbortError') {
                        observer.error(error);
                    }
                });

            return () => controller.abort();
        });
    }
}
