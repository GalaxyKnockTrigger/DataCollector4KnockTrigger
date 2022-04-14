package com.capstone.knockdatareceiver4watch.listener;

import android.hardware.SensorEventListener;

import java.util.LinkedList;
import java.util.Queue;

public abstract class IMUListener implements SensorEventListener, IGetDataAsCSV, IGetDataSize {
    protected final Queue<float[]> dataQueue;

    public IMUListener() {
        dataQueue = new LinkedList<>();
    }
    public abstract int getDataSize();

    public abstract String getDataAsCSV ();
}