package com.pardal.kitapligim;
import java.time.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReminderTimeTest {
    private static final ZoneId TR=ZoneId.of("Europe/Istanbul");
    @Test public void beforeElevenSchedulesTonight(){assertEquals(Instant.parse("2026-11-14T20:00:00Z").toEpochMilli(),ReminderTime.next(Instant.parse("2026-11-14T19:59:00Z"),TR,23,0));}
    @Test public void afterOrAtElevenSchedulesTomorrow(){for(String now:new String[]{"2026-11-14T20:00:00Z","2026-11-14T22:00:00Z"})assertEquals(Instant.parse("2026-11-15T20:00:00Z").toEpochMilli(),ReminderTime.next(Instant.parse(now),TR,23,0));}
    @Test public void timezoneAndDstFollowTheLocalWallClock(){ZoneId zone=ZoneId.of("Europe/Berlin");assertEquals(Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(),ReminderTime.next(Instant.parse("2026-03-29T00:00:00Z"),zone,2,30));}
    @Test public void tomorrowCrossesTheYearBoundary(){assertEquals(Instant.parse("2027-01-01T20:00:00Z").toEpochMilli(),ReminderTime.next(Instant.parse("2026-12-31T21:00:00Z"),TR,23,0));}
    @Test public void oldAlarmsDoNotShowThePreviousEveningReminderNextMorning(){Instant at=Instant.parse("2026-11-14T20:00:00Z");assertTrue(ReminderTime.timely(at,at.plusSeconds(600),TR));assertFalse(ReminderTime.timely(at,at.plusSeconds(7200),TR));assertFalse(ReminderTime.timely(at,at.minusSeconds(1),TR));}
    @Test public void invalidClockValuesFail(){assertThrows(DateTimeException.class,()->ReminderTime.next(Instant.now(),TR,24,0));assertThrows(DateTimeException.class,()->ReminderTime.next(Instant.now(),TR,23,60));}
}
