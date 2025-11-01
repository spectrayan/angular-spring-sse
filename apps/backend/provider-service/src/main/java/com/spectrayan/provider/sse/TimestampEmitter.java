package com.spectrayan.provider.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimestampEmitter {
    private static final Logger log = LoggerFactory.getLogger(TimestampEmitter.class);

    private final UserSseService userSseService;

    public TimestampEmitter(UserSseService userSseService) {
        this.userSseService = userSseService;
    }

    @Scheduled(fixedRate = 5000)
    public void tick() {
        String ts = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String msg = "time=" + ts;
        if (!userSseService.currentUsers().isEmpty()) {
            log.debug("Emitting timestamp to {} user(s) {}: {}", userSseService.currentUsers().size(), userSseService.currentUsers(),msg);
        }
        userSseService.emitToAll(msg);
    }
}
