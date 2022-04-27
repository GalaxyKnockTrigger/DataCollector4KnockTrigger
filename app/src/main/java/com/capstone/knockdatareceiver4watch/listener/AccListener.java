package com.capstone.knockdatareceiver4watch.listener;

import static com.capstone.knockdatareceiver4watch.Constants.RECEIVING_TIME;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.FloatFFT_1D;

import java.util.List;

public
class AccListener extends IMUListener {
    public AccListener() {
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
        Log.i("ACC_RAW_VAL", ret.toString());

        return ret.toString();
    }

    public void doFFT(){
        final int SAMPLE_RATE = 100;

        final int ZERO_PADDING_SIZE = 200;

        final int INPUT_SAMPLE_SIZE = 16;


        int blockSize = INPUT_SAMPLE_SIZE + ZERO_PADDING_SIZE;

        double[][] toTransform = new double[3][2*blockSize];   // Real + Imagine
        double[][] mag = new double[3][blockSize];             // toTransform_size / 2

        int i = 0;
        for(Object arr : dataQueue.toArray()){
            for(int j = 0; j < 3; j++){
                toTransform[j][2*i] = ((float[])arr)[j];
                toTransform[j][2*i + 1] = 0;
            }
            i+=1;

            if(i > INPUT_SAMPLE_SIZE)
                break;
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);
        for(i = 0; i<3; i++)
            fft.complexForward(toTransform[i]);

        for(int j = 0; j < 3; j++){
            for(i = 0; i < blockSize / 2; i++){
                mag[j][i] = Math.sqrt(Math.pow(toTransform[j][2*i],2) + Math.pow(toTransform[j][2*i + 1], 2));
            }
        }

        StringBuilder[] fftResult = new StringBuilder[3];

        for(i = 0; i < 3; i++){
            fftResult[i] = new StringBuilder();
            for(double val : mag[i]){
                fftResult[i].append(val).append(',');
            }
        }
        Log.i("ACC_FFT_VAL_X", fftResult[0].toString());
        Log.i("ACC_FFT_VAL_Y", fftResult[1].toString());
        Log.i("ACC_FFT_VAL_Z", fftResult[2].toString());
//
//        for(i = 0; i<)

    }
}