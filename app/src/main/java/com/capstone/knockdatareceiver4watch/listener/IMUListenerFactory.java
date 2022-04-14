package com.capstone.knockdatareceiver4watch.listener;

import static com.capstone.knockdatareceiver4watch.Constants.TYPE_ACCEL;
import static com.capstone.knockdatareceiver4watch.Constants.TYPE_GYRO;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class IMUListenerFactory {

    public static IMUListener getListener(int TYPE){
        if(TYPE == TYPE_ACCEL){
            return new AccListener();
        } else if (TYPE == TYPE_GYRO){
            return new GyroListener();
        }
        return null;
    }
}

