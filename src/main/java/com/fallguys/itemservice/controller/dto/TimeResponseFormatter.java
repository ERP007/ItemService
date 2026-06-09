package com.fallguys.itemservice.controller.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class TimeResponseFormatter {

    private TimeResponseFormatter() {
    }

    static String format(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toString();
    }

    static String formatDate(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
    }
}
