package com.pardal.kitapligim;

import java.time.*;

/** Local wall-clock reminders follow the phone's timezone, including daylight-saving changes. */
public final class ReminderTime {
    public static long next(Instant now, ZoneId zone, int hour, int minute) {
        LocalTime time=LocalTime.of(hour,minute);
        LocalDate day=now.atZone(zone).toLocalDate();
        ZonedDateTime next=day.atTime(time).atZone(zone);
        if(!next.toInstant().isAfter(now))next=day.plusDays(1).atTime(time).atZone(zone);
        return next.toInstant().toEpochMilli();
    }
    static boolean timely(Instant scheduled,Instant now,ZoneId zone){
        return !now.isBefore(scheduled)&&Duration.between(scheduled,now).toMinutes()<180&&scheduled.atZone(zone).toLocalDate().equals(now.atZone(zone).toLocalDate());
    }
}
