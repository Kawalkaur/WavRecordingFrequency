package com.cdac.wavrecording;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import uk.me.berndporr.iirj.Butterworth;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 4000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 1024;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    int dispatcherBufferSize = 1024;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setButtonHandlers();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(4000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    private void setButtonHandlers() {
        ( findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ( findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        (findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    private String getFilename() {
        String filepath =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/"+ AUDIO_RECORDER_FOLDER;
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

       /* if(!file.exists()){
            file.mkdirs();
        }*/

        return (filepath+ "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename() {
        String filepath =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/"+ AUDIO_RECORDER_FOLDER;
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

     /*   if(tempFile.exists())
            tempFile.delete();*/

        return (filepath + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording() {
        /*if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }*/
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 0);
            return;
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if (i == 1) {
            recorder.startRecording();
        }

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }


  /*  private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read;
        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);

                Butterworth butterworth = new Butterworth();
                int highcutoff = 500;
                int lowcutoff = 2000;
                double centerFreq = (highcutoff + lowcutoff) / 2.0;
                double width = Math.abs(highcutoff - lowcutoff);
                butterworth.bandPass(4, RECORDER_SAMPLERATE, centerFreq, width);

                if (read > 0) {
                    short[] shorts = new short[read / 2];
                    ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);
                    double[] doubleData = new double[read / 2];
                    for (int i = 0; i < read / 2; i++) {
                        doubleData[i] = shorts[i] / 32768.0;
                    }

                    double[] filteredData = new double[doubleData.length];
                    int index = 0;
                    for (double sample : doubleData) {
                        double filteredSample = butterworth.filter(sample);
                        filteredData[index++] = filteredSample;
                    }

                    byte[] filteredBytes = new byte[read];
                    for (int i = 0; i < read / 2; i++) {
                        short filteredSample = (short) (filteredData[i] * 32768.0);
                        filteredBytes[i * 2] = (byte) filteredSample;
                        filteredBytes[i * 2 + 1] = (byte) (filteredSample >> 8);
                    }
                    if (os != null) {
                        try {
                            os.write(filteredBytes, 0, read);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    private void writeAudioDataToFile() {
        byte[] data = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;
        Butterworth butterworth = new Butterworth();
        int highcutoff = 500;
        int lowcutoff = 2000;
        double centerFreq = (highcutoff + lowcutoff) / 2.0;
        double width = Math.abs(highcutoff - lowcutoff);
        butterworth.bandPass(4, RECORDER_SAMPLERATE, centerFreq, width);

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read;
        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);

                if (read > 0) {
                    short[] shorts = new short[read / 2];
                    ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);
                    double[] doubleData = new double[read / 2];
                    for (int i = 0; i < read / 2; i++) {
                        doubleData[i] = shorts[i] / 32768.0;
                    }

                    double[] filteredData = new double[doubleData.length];
                    int index = 0;
                    for (double sample : doubleData) {
                        double filteredSample = butterworth.filter(sample);
                        filteredData[index++] = filteredSample;
                    }

                    byte[] filteredBytes = new byte[read];
                    for (int i = 0; i < read / 2; i++) {
                        short filteredSample = (short) (filteredData[i] * 32768.0);
                        filteredBytes[i * 2] = (byte) (filteredSample & 0xff);
                        filteredBytes[i * 2 + 1] = (byte) ((filteredSample >> 8) & 0xff);
                    }

                    try {
                        os.write(filteredBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }
        isRecording = false;

        int i = recorder.getState();
        if (i == 1) {
            recorder.stop();
        }
        recorder.release();
        recorder = null;
        recordingThread = null;

        copyWaveFile(getTempFilename(), getFilename());
        //deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen = 0;
        long totalDataLen;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[dispatcherBufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(inFilename);
        //file.delete();
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) ((long) MainActivity.RECORDER_SAMPLERATE & 0xff);
        header[25] = (byte) (((long) MainActivity.RECORDER_SAMPLERATE >> 8) & 0xff);
        header[26] = (byte) (((long) MainActivity.RECORDER_SAMPLERATE >> 16) & 0xff);
        header[27] = (byte) (((long) MainActivity.RECORDER_SAMPLERATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener btnClick = v -> {
        switch (v.getId()) {
            case R.id.btnStart: {
                AppLog.logString("Start Recording");

                enableButtons(true);
                startRecording();

                break;
            }
            case R.id.btnStop: {
                AppLog.logString("Stop Recording");

                enableButtons(false);
                stopRecording();

                break;
            }
        }
    };
}

