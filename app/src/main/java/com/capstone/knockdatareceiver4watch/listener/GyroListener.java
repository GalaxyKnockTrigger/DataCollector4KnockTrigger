package com.capstone.knockdatareceiver4watch.listener;

import static com.capstone.knockdatareceiver4watch.Constants.RECEIVING_TIME;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.util.List;

public class GyroListener extends IMUListener {
    public GyroListener() {
        super();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values = { sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2] };
        dataQueue.add(values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public int getDataSize(){
        return dataQueue.size();
    }
    public List<float[]> getData(){
        return dataQueue;
    }
    public String getDataAsCSV (){
        StringBuilder ret = new StringBuilder("#x, y, z\n");
//            long start = timestamps.peek();
        int i = 0;
        while(i < RECEIVING_TIME * 0.1){
//                ret.append(timestamps.poll() - start);

            float[] values = dataQueue.get(i);
            assert values != null;
            ret.append(values[0]).append(',').append(values[1]).append(',').append(values[2]).append("\n");
            i++;
        }

        Log.i("GYRO_RAW_VAL", ret.toString());
        return ret.toString();
    }
}
