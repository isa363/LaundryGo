package com.example.laundryproject.home;

public class MachineItem {

    public String machineId;
    public String machineName;
    public String state;
    public Long   epochStart;
    public String timestamp;
    public long   elapsedSeconds;

    public MachineItem(String machineId, String machineName,
                       String state, Long epochStart, String timestamp) {
        this.machineId      = machineId;
        this.machineName    = machineName;
        this.state          = state;
        this.epochStart     = epochStart;
        this.timestamp      = timestamp;
        this.elapsedSeconds = 0;
    }

}
