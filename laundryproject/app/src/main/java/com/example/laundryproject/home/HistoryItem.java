package com.example.laundryproject.home;

public class HistoryItem {

    public String machineName;
    public long   epochSeconds;
    public double durationMin;
    public double costUSD;

    public HistoryItem(String machineName, long epochSeconds,
                       double durationMin, double costUSD) {
        this.machineName  = machineName;
        this.epochSeconds = epochSeconds;
        this.durationMin  = durationMin;
        this.costUSD      = costUSD;
    }

}
