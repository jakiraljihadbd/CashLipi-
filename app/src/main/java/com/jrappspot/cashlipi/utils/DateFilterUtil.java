package com.jrappspot.cashlipi.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Shared filter logic for "সব / আজ / সপ্তাহ / মাস / বছর" chips.
 * dateStr is expected in "yyyy-MM-dd" format.
 */
public class DateFilterUtil {

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static boolean matches(String dateStr, String filter) {
        if ("all".equals(filter) || filter == null) return true;
        if (dateStr == null || dateStr.isEmpty()) return false;

        Date d;
        try {
            d = FMT.parse(dateStr);
        } catch (Exception e) {
            return false;
        }
        if (d == null) return false;

        Calendar target = Calendar.getInstance();
        target.setTime(d);

        Calendar now = Calendar.getInstance();

        switch (filter) {
            case "today":
                return sameDay(target, now);
            case "week":
                return sameWeek(target, now);
            case "month":
                return target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && target.get(Calendar.MONTH) == now.get(Calendar.MONTH);
            case "year":
                return target.get(Calendar.YEAR) == now.get(Calendar.YEAR);
            default:
                return true;
        }
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean sameWeek(Calendar a, Calendar b) {
        // Align both calendars to start-of-week (Sunday) for a stable comparison
        Calendar ca = (Calendar) a.clone();
        Calendar cb = (Calendar) b.clone();
        ca.setFirstDayOfWeek(Calendar.SUNDAY);
        cb.setFirstDayOfWeek(Calendar.SUNDAY);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.WEEK_OF_YEAR) == cb.get(Calendar.WEEK_OF_YEAR);
    }
}
