package com.capstone.knockdatareceiver4watch;

import android.Manifest;
import android.hardware.Sensor;

public class Constants {
    public static final int TYPE_ACCEL = Sensor.TYPE_ACCELEROMETER;
    public static final int TYPE_GYRO = Sensor.TYPE_GYROSCOPE;
    public static final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    public static final int RCODE = 10000;
    public static final int RECEIVING_TIME = 2000;
    public static final String URL = "https://nas.splo2t.com";
    public static final int PORT = 9999;


    public static final short TH_SOUND_PEAK = 5000;
}
