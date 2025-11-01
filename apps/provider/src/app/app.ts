import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SseService, SseStatus } from './sse.service';

@Component({
  imports: [CommonModule],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnDestroy {
  users = [
    { id: 'john', name: 'John Doe' },
    { id: 'alice', name: 'Alice' },
    { id: 'bob', name: 'Bob' },
  ];

  selectedUser: { id: string; name: string } | null = null;
  messages: string[] = [];
  status: SseStatus = 'disconnected';

  constructor(private sse: SseService) {
    this.sse.messages$.subscribe((m) => {
      this.messages.unshift(m);
      if (this.messages.length > 200) this.messages.pop();
    });
    this.sse.status$.subscribe((s) => (this.status = s));
  }

  selectUser(u: { id: string; name: string }) {
    if (this.selectedUser?.id === u.id) return;
    this.selectedUser = u;
    this.messages = [];
    this.sse.connect(u.id);
  }

  disconnect() {
    this.sse.disconnect();
    this.selectedUser = null;
    this.status = 'disconnected';
  }

  clearMessages() {
    this.messages = [];
  }

  ngOnDestroy(): void {
    this.sse.disconnect();
  }
}
