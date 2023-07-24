package com.norton;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class DateTimeFormatters {

    public static final DateTimeFormatter MIDNIGHT = new DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral("-")
            .appendMonthOfYear(2)
            .appendLiteral("-")
            .appendDayOfMonth(2)
            .appendLiteral("T00:00:00")
            .toFormatter();
    public static final DateTimeFormatter TSA = new DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral("-")
            .appendMonthOfYear(2)
            .appendLiteral("-")
            .appendDayOfMonth(2)
            .appendLiteral("T")
            .appendHourOfDay(2)
            .appendLiteral(":")
            .appendMinuteOfHour(2)
            .appendLiteral(":")
            .appendSecondOfMinute(2)
            .toFormatter();

    public static final DateTimeFormatter DATE_ONLY = new DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendLiteral("-")
            .appendMonthOfYear(2)
            .appendLiteral("-")
            .appendDayOfMonth(2)
            .toFormatter();

}


