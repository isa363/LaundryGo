package com.example.laundryproject.home;

import com.google.firebase.database.DataSnapshot;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class AdminAnalyticsCalculator {

    private AdminAnalyticsCalculator() {}

    public static class Result {
        public int[][] dayHourCounts = new int[7][24];
        public float[] hourlyAverages = new float[24];

        public int totalCycles = 0;
        public double totalRevenue = 0.0;

        public int peakHour = -1;
        public int peakHourCycles = 0;

        public String topMachineName = "—";
        public int topMachineCycles = 0;

        public int activeMachineCount = 0;
    }

    public static Result fromSnapshot(DataSnapshot snapshot, String adminBuilding) {
        Result result = new Result();
        Map<String, Integer> machineCycleCounts = new TreeMap<>();

        for (DataSnapshot machine : snapshot.getChildren()) {

            // skip random broken nodes / strings / mixed junk
            if (!machine.hasChild("buildingCode") && !machine.hasChild("machineName")) {
                continue;
            }

            String building = machine.child("buildingCode").getValue(String.class);
            if (adminBuilding != null && !adminBuilding.trim().isEmpty()) {
                if (building == null || !adminBuilding.equals(building)) {
                    continue;
                }
            }

            result.activeMachineCount++;

            String machineName = machine.child("machineName").getValue(String.class);
            if (machineName == null || machineName.trim().isEmpty()) {
                machineName = machine.getKey();
            }
            if (machineName == null || machineName.trim().isEmpty()) {
                machineName = "Unknown Machine";
            }

            Double machinePrice = machine.child("price").getValue(Double.class);
            if (machinePrice == null) {
                machinePrice = 0.0;
            }

            int machineCycles = 0;

            DataSnapshot historyNode = machine.child("history");
            for (DataSnapshot entry : historyNode.getChildren()) {

                Long epoch = entry.child("epoch").getValue(Long.class);
                if (epoch == null) {
                    continue;
                }

                Double costUSD = entry.child("costUSD").getValue(Double.class);
                double cost = costUSD != null ? costUSD : machinePrice;

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(epoch * 1000L);

                int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                int hour = calendar.get(Calendar.HOUR_OF_DAY);

                result.dayHourCounts[day][hour]++;
                result.totalCycles++;
                result.totalRevenue += cost;
                machineCycles++;
            }

            machineCycleCounts.put(machineName, machineCycles);
        }

        computeHourlyAverages(result);
        computePeakHour(result);
        computeTopMachine(result, machineCycleCounts);

        return result;
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

    private static void computePeakHour(Result result) {
        int max = 0;
        int bestHour = -1;

        for (int hour = 0; hour < 24; hour++) {
            int totalForHour = 0;
            for (int day = 0; day < 7; day++) {
                totalForHour += result.dayHourCounts[day][hour];
            }

            if (totalForHour > max) {
                max = totalForHour;
                bestHour = hour;
            }
        }

        result.peakHour = bestHour;
        result.peakHourCycles = max;
    }

    private static void computeTopMachine(Result result, Map<String, Integer> machineCycleCounts) {
        String bestMachine = "—";
        int maxCycles = 0;

        for (Map.Entry<String, Integer> entry : machineCycleCounts.entrySet()) {
            if (entry.getValue() > maxCycles) {
                maxCycles = entry.getValue();
                bestMachine = entry.getKey();
            }
        }

        result.topMachineName = bestMachine;
        result.topMachineCycles = maxCycles;
    }

    public static String formatHourRange(int hour24) {
        if (hour24 < 0) return "—";
        return String.format(Locale.US, "%s - %s",
                formatHour(hour24),
                formatHour((hour24 + 1) % 24));
    }

    public static String formatHour(int hour24) {
        int displayHour = hour24 % 12 == 0 ? 12 : hour24 % 12;
        String suffix = hour24 < 12 ? "AM" : "PM";
        return displayHour + " " + suffix;
    }
}