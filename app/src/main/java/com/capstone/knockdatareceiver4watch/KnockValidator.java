package com.capstone.knockdatareceiver4watch;

import static com.capstone.knockdatareceiver4watch.Constants.TH_SOUND_PEAK;

import java.util.List;
import java.util.Optional;

public class KnockValidator {
    private List<Short> audioData;
    private List<float[]> accData;
    private List<float[]> gyroData;

    private List<Short> outputAudioData;
    private List<float[]> outputAccData;
    private List<float[]> outputGyroData;
    private int soundPeak = -1;

    public KnockValidator(List<Short> audioData, List<float[]> accData, List<float[]> gyroData){
        this.audioData = audioData;
        this.accData = accData;
        this.gyroData = gyroData;

        peakDetection();
        responsePruning();
    }

    public List<Short> getAudioData(){
        return outputAudioData;
    }
    public List<float[]> getAccData(){
        return outputAccData;
    }
    public List<float[]> getGyroData(){
        return getGyroData();
    }

    private void peakDetection(){
        for(int i = 0; i < audioData.size(); i++){
            if(audioData.get(i) > TH_SOUND_PEAK){
                outputAudioData = audioData.subList(i, i + 4096);
                soundPeak = i;
                break;
            }
        }
    }

    private void responsePruning(){
        final int peakStartIdx = soundPeak / 480; // peak * 100 / (48000)

        final int searchEndIdx = 75 + peakStartIdx;

        int searchStartIdx = searchEndIdx - 15;

        int idx = searchStartIdx;
        float maxVal = accData.get(idx)[2];
        int maxIdx = idx;
        for(; idx <= searchEndIdx; idx++){
            if(accData.get(idx)[2] > maxVal){
                maxVal = accData.get(idx)[2];
                maxIdx = idx;
            }
        }

        outputAccData = accData.subList(maxIdx, maxIdx + 8);
        outputGyroData = gyroData.subList(maxIdx, maxIdx + 8);
    }
}
