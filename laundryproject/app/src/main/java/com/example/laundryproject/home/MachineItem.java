package com.example.laundryproject.home;

public class MachineItem {

    public String machineId = "";
    public String machineName = "";
    public String state = "DISCONNECTED";
    public Long epochStart;
    public String timestamp = "";
    public double price = 0.0;

    public long elapsedSeconds = 0L;
    public long lastUpdatedAt;

    public String buildingCode  = ""; // NEW FIELD

    public MachineItem() {}

    // BACKWARD COMPATIBLE CONSTRUCTOR
    public MachineItem(String machineID, String machineName,
                       String state, long epoch, String timestamp) {

        this.machineId = machineID != null ? machineID : "";
        this.machineName = machineName != null ? machineName : "";
        this.state = state != null ? state : "DISCONNECTED";
        this.epochStart = epoch;
        this.timestamp = timestamp != null ? timestamp : "";
    }

    // FULL CONSTRUCTOR
    public MachineItem(String machineID, String machineName,
                       String state, long epoch, String timestamp,
                       double price, String buildingName) {

        this.machineId = machineID != null ? machineID : "";
        this.machineName = machineName != null ? machineName : "";
        this.state = state != null ? state : "DISCONNECTED";
        this.epochStart = epoch;
        this.timestamp = timestamp != null ? timestamp : "";
        this.price = price;
        this.buildingCode  = buildingName != null ? buildingName : "";
    }
}