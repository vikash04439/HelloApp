package com.learn.rest.HelloApp.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generates unique transaction IDs in format: DDMMYYYY-HHMM-XXXXXX
 * XXXXXX is a sequential counter starting from 000001, resets when the minute changes.
 */
public class TransactionIdGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final AtomicReference<String> currentMinute = new AtomicReference<>("");

    public static String generate(String applicationNode) {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_FMT);
        String timePart = now.format(TIME_FMT);
        String minuteKey = datePart + "-" + timePart;

        // Reset counter if minute has changed
        String prev = currentMinute.get();
        if (!minuteKey.equals(prev)) {
            if (currentMinute.compareAndSet(prev, minuteKey)) {
                counter.set(0);
            }
        }

        int seq = counter.incrementAndGet();
        String node = (applicationNode != null && !applicationNode.isEmpty()) ? applicationNode : "";
        return String.format("%s-%s-%s%06d", datePart, timePart, node, seq);
    }
}

