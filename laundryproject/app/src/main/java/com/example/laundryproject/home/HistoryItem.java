package com.example.laundryproject.home;

public class HistoryItem {

    private final String machineName;
    private final long epoch;
    private final double duration;
    private final double cost;

    public HistoryItem(String machineName, long epoch, double duration, double cost) {
        this.machineName = machineName;
        this.epoch = epoch;
        this.duration = duration;
        this.cost = cost;
    }

    public String getMachineName() {
        return machineName;
    }

    public long getEpoch() {
        return epoch;
    }

    public double getDuration() {
        return duration;
    }

    public double getCost() {
        return cost;
    }
}