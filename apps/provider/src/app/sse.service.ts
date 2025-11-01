import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

export type SseStatus = 'connecting' | 'connected' | 'disconnected';

@Injectable({ providedIn: 'root' })
export class SseService {
  private es: EventSource | null = null;
  private messagesSubject = new Subject<string>();
  private statusSubject = new BehaviorSubject<SseStatus>('disconnected');
  private currentUserId: string | null = null;
  private reconnectTimer: any = null;
  private reconnectAttempts = 0;

  readonly messages$: Observable<string> = this.messagesSubject.asObservable();
  readonly status$: Observable<SseStatus> = this.statusSubject.asObservable();

  connect(userId: string) {
    // Clean up any existing connection and timers
    this.disconnect();

    this.currentUserId = userId;
    this.reconnectAttempts = 0; // reset attempts for a fresh connection
    this.statusSubject.next('connecting');

    const url = `/sse/${encodeURIComponent(userId)}`;
    const es = new EventSource(url);
    this.es = es;

    es.onopen = () => {
      this.reconnectAttempts = 0; // reset on successful open
      this.statusSubject.next('connected');
    };

    es.onmessage = (ev) => {
      if (ev?.data) {
        this.messagesSubject.next(ev.data);
      }
    };

    es.addEventListener('tick', (ev: MessageEvent) => {
      if (ev?.data) this.messagesSubject.next(ev.data);
    });

    es.onerror = () => {
      // Move to a controlled reconnect flow
      // Close the current EventSource to avoid browser's opaque auto-retry hiding our state
      this.cleanupEventSource();
      // If a user is still selected, try to reconnect with backoff
      if (this.currentUserId) {
        this.statusSubject.next('connecting');
        this.scheduleReconnect();
      } else {
        this.statusSubject.next('disconnected');
      }
    };
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.cleanupEventSource();
    this.statusSubject.next('disconnected');
    // Prevent any scheduled reconnects after an explicit manual disconnect
    this.currentUserId = null;
    this.reconnectAttempts = 0;
  }

  private cleanupEventSource() {
    if (this.es) {
      try {
        this.es.close();
      } catch {}
      this.es = null;
    }
  }

  private scheduleReconnect() {
    if (!this.currentUserId) return;
    const delay = Math.min(30000, 1000 * Math.pow(2, this.reconnectAttempts)); // 1s, 2s, 4s... max 30s
    this.reconnectAttempts++;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => {
      if (this.currentUserId) {
        this.connect(this.currentUserId);
      }
    }, delay);
  }
}
