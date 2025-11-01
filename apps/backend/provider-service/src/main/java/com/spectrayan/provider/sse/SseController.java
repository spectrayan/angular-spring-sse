package com.spectrayan.provider.sse;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final UserSseService userSseService;

    public SseController(UserSseService userSseService) {
        this.userSseService = userSseService;
    }

    @GetMapping(path = "/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@PathVariable String userId) {
        return userSseService.connect(userId)
                .map(data -> ServerSentEvent.<String>builder()
                        .event("tick")
                        .data(data)
                        .build())
                .doOnSubscribe(s -> userSseService.emitToUser(userId, "connected"));
    }
}
