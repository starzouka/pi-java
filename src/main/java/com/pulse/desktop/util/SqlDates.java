package com.pulse.desktop.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SqlDates {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private SqlDates() {
    }

    public static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    public static Timestamp nowTs() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return DATE_TIME_FORMAT.format(dateTime);
    }
}
