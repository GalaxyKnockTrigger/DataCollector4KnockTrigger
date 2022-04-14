package com.capstone.knockdatareceiver4watch.listener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class GyroListener extends IMUListener {
    public GyroListener() {
        super();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values = { sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2] };
        dataQueue.offer(values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public int getDataSize(){
        return dataQueue.size();
    }

    public String getDataAsCSV (){
        StringBuilder ret = new StringBuilder("#x, y, z\n");
//            long start = timestamps.peek();
        int i = 0;
        while(i < 32){
//                ret.append(timestamps.poll() - start);

            float[] values = dataQueue.poll();
            assert values != null;
            ret.append(values[0]).append(',').append(values[1]).append(',').append(values[2]).append("\n");
            i++;
        }
        return ret.toString();
    }
}
