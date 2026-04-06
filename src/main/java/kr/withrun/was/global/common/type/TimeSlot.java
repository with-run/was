package kr.withrun.was.global.common.type;

import java.time.LocalTime;

public enum TimeSlot {
    DAWN(LocalTime.MIN, LocalTime.of(6, 0)),
    MORNING(LocalTime.of(6, 0), LocalTime.NOON),
    AFTERNOON(LocalTime.NOON, LocalTime.of(18, 0)),
    EVENING(LocalTime.of(18, 0), LocalTime.MAX);

    private final LocalTime start;
    private final LocalTime end;

    TimeSlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public boolean matches(LocalTime time) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    public static TimeSlot from(LocalTime time) {
        for (TimeSlot slot : values()) {
            if (slot.matches(time)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("Invalid time: " + time);
    }
}
