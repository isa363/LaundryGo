package com.example.laundryproject.home;

import com.google.firebase.database.DataSnapshot;

import java.util.Calendar;
import java.util.Locale;

public final class UsageInsightCalculator {

    public static final String FILTER_ALL = "ALL";
    public static final String TIME_ALL = "ALL";
    public static final String TIME_WEEK = "WEEK";
    public static final String TIME_MONTH = "MONTH";
    public static final String FILTER_WASHERS = "WASHERS";
    public static final String FILTER_DRYERS = "DRYERS";

    private UsageInsightCalculator() {}

    public static class Result {
        public int[][] dayHourCounts = new int[7][24];
        public float[] hourlyAverages = new float[24];
        public int totalSessions = 0;
        public int peakDay = -1;
        public int peakHour = -1;
        public int quietDay = -1;
        public int quietHour = -1;

        public String getPeakLabel() {
            return formatDayHour(peakDay, peakHour);
        }

        public String getQuietLabel() {
            return formatDayHour(quietDay, quietHour);
        }

        private String formatDayHour(int day, int hour) {
            if (day < 0 || hour < 0) return "—";
            String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            return days[day] + " • " + formatHour(hour);
        }
    }

    public static Result fromSnapshot(DataSnapshot snapshot, String buildingCode, String machineFilter, String timeFilter) {
        Result result = new Result();

        for (DataSnapshot machine : snapshot.getChildren()) {

            String machineBuilding = machine.child("buildingCode").getValue(String.class);
            if (machineBuilding == null || !machineBuilding.equals(buildingCode)) {
                continue;
            }

            String machineType = resolveMachineType(machine);
            if (!matchesFilter(machineType, machineFilter)) {
                continue;
            }

            DataSnapshot historyNode = machine.child("history");

            for (DataSnapshot entry : historyNode.getChildren()) {
                Long epoch = entry.child("epoch").getValue(Long.class);
                if (epoch == null) continue;


                if (!matchesTimeFilter(epoch, timeFilter)) continue;

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(epoch * 1000L);

                int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                int hour = calendar.get(Calendar.HOUR_OF_DAY);

                result.dayHourCounts[day][hour]++;
                result.totalSessions++;
            }
        }

        computePeakAndQuiet(result);
        computeHourlyAverages(result);

        return result;
    }

    private static void computePeakAndQuiet(Result result) {
        int maxCount = -1;
        int minPositiveCount = Integer.MAX_VALUE;

        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) {
                int count = result.dayHourCounts[day][hour];

                if (count > maxCount) {
                    maxCount = count;
                    result.peakDay = day;
                    result.peakHour = hour;
                }

                if (count > 0 && count < minPositiveCount) {
                    minPositiveCount = count;
                    result.quietDay = day;
                    result.quietHour = hour;
                }
            }
        }
    }

    private static boolean matchesTimeFilter(long epochSeconds, String timeFilter) {
        if (TIME_ALL.equals(timeFilter)) return true;

        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone();

        if (TIME_WEEK.equals(timeFilter)) {
            start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        } else if (TIME_MONTH.equals(timeFilter)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
        }

        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        return epochSeconds >= start.getTimeInMillis() / 1000L;
    }

    private static void computeHourlyAverages(Result result) {
        for (int hour = 0; hour < 24; hour++) {
            int totalForHour = 0;
            for (int day = 0; day < 7; day++) {
                totalForHour += result.dayHourCounts[day][hour];
            }
            result.hourlyAverages[hour] = totalForHour / 7f;
        }
    }

    private static boolean matchesFilter(String machineType, String filter) {
        if (FILTER_ALL.equals(filter)) return true;
        if (FILTER_WASHERS.equals(filter)) return "WASHER".equals(machineType);
        if (FILTER_DRYERS.equals(filter)) return "DRYER".equals(machineType);
        return true;
    }

    private static String resolveMachineType(DataSnapshot machine) {
        String explicitType = firstNonNull(
                machine.child("type").getValue(String.class),
                machine.child("machineType").getValue(String.class),
                machine.child("category").getValue(String.class)
        );

        if (explicitType != null) {
            String normalized = explicitType.trim().toLowerCase(Locale.US);
            if (normalized.contains("dryer")) return "DRYER";
            if (normalized.contains("washer") || normalized.contains("wash")) return "WASHER";
        }

        String name = machine.child("machineName").getValue(String.class);
        if (name == null) name = machine.getKey();
        if (name == null) return "UNKNOWN";

        String normalizedName = name.toLowerCase(Locale.US);
        if (normalizedName.contains("dryer")) return "DRYER";
        if (normalizedName.contains("washer") || normalizedName.contains("wash")) return "WASHER";

        return "UNKNOWN";
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    public static String formatHour(int hour24) {
        int displayHour = hour24 % 12 == 0 ? 12 : hour24 % 12;
        String suffix = hour24 < 12 ? "AM" : "PM";
        return displayHour + " " + suffix;
    }


}