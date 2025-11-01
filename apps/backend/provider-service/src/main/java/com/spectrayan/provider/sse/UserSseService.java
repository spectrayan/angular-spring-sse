package com.spectrayan.provider.sse;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages per-user reactive sinks and subscriptions for SSE streaming.
 */
@Service
public class UserSseService {

    private static final Logger log = LoggerFactory.getLogger(UserSseService.class);

    private static class UserChannel {
        final Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();
        final AtomicInteger subscribers = new AtomicInteger(0);
    }

    private final Map<String, UserChannel> channels = new ConcurrentHashMap<>();

    /**
     * Obtain a Flux for the given user. Channel is created on first access.
     */
    public Flux<String> connect(String userId) {
        UserChannel channel = channels.computeIfAbsent(userId, id -> new UserChannel());

        Flux<String> sinkFlux = channel.sink.asFlux();
        // Heartbeat tied to lifecycle of the sink so it cancels when the sink completes
        Flux<String> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(t -> "::heartbeat::")
                .takeUntilOther(sinkFlux.ignoreElements());

        return Flux.merge(sinkFlux, heartbeat)
                .doOnSubscribe(sub -> channel.subscribers.incrementAndGet())
                .doFinally(sig -> {
                    int left = channel.subscribers.decrementAndGet();
                    if (left <= 0) {
                        // complete and remove to free resources
                        channel.sink.tryEmitComplete();
                        channels.remove(userId);
                    }
                });
    }

    /**
     * Emit a message for a specific user. If user channel does not exist, nothing happens.
     */
    public void emitToUser(String userId, String message) {
        UserChannel channel = channels.get(userId);
        if (channel != null) {
            channel.sink.tryEmitNext(message);
        }
    }

    /**
     * Emit the message to all users currently connected.
     */
    public void emitToAll(String message) {
        channels.forEach((id, channel) -> channel.sink.tryEmitNext(message));
    }

    public Collection<String> currentUsers() {
        return channels.keySet();
    }

    /**
     * Gracefully close all active SSE channels on application shutdown to avoid
     * blocking graceful shutdown with in-flight requests.
     */
    @PreDestroy
    public void shutdown() {
        int count = channels.size();
        if (count > 0) {
            log.info("Shutting down UserSseService: completing {} SSE channel(s)", count);
        } else {
            log.info("Shutting down UserSseService: no active SSE channels");
        }
        channels.forEach((id, ch) -> {
            try {
                ch.sink.tryEmitComplete();
            } catch (Throwable t) {
                log.warn("Error completing SSE channel for user {}: {}", id, t.getMessage());
            }
        });
        channels.clear();
    }
}
