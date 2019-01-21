package com.example.android.colseasons;

public class TempStamp {
    private long timestamp;
    private double random;

    public TempStamp(long timestamp, double random)
    {
        this.timestamp = timestamp;
        this.random = random;
    }

    public long getTimestamp()
    {
        return this.timestamp;
    }

    public double getRandom()
    {
        return this.random;
    }
}
