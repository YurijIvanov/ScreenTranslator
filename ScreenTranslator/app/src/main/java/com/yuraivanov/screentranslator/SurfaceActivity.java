package com.yuraivanov.screentranslator;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class SurfaceActivity extends AppCompatActivity {
    private static final String TAG = "SurfaceActivity";
    private static ImageView screen;
    //private static AudioTrack audioTrack;
    private static boolean receiving=true;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity=this;
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_surface);
        screen=findViewById(R.id.screen);
        Button button = findViewById(R.id.stopReceiving);
        button.setOnClickListener(view ->{
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
    }
    //public static void stopSurface(){
    //    if(activity!=null){
    //        activity.finish();
    //    }
    //}
    public static void startReceiving(){
        InputStream videoInput/*,audioInput*/;
        /*int bufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 4100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);*/
        videoInput=SocketTask.getVideoInput();
        //audioInput=SocketTask.getAudioInput();
        if(videoInput==null /*|| audioInput==null*/){
            stopReceiving();
        }else{
            receiving=true;
            new Thread(() -> {
                Log.d(TAG,"videothread start");
                while (receiving){
                    ObjectInputStream ois;
                    try {
                        ois = new ObjectInputStream(videoInput);
                    } catch (IOException e) {
                        Log.e(TAG,"new ObjectInputStream " + e.getMessage());
                        stopReceiving();
                        return;
                    }
                    Object obj = null;
                    try {
                        obj = ois.readObject();
                    } catch (IOException e) {
                        Log.e(TAG,"ois.readObject IOException " + e.getMessage());
                        //stopServer();
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG,"ois.readObject ClassNotFoundException " + e.getMessage());
                    }
                    if(obj!=null){
                        byte[] imageBytes = (byte[]) obj;
                        Bitmap image = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
                        handler.post(()->screen.setImageBitmap(image));
                    }
                }
                Log.d(TAG,"videothread end");
            }).start();
            /*new Thread(() -> {
                /*try {
                    byte[] buffer = new byte[bufferSize];
                    int numRead;
                    while (receiving && (numRead = audioInput.read(buffer)) != -1){
                        audioTrack.write(buffer,0,numRead);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "audioThread error " + e.getMessage());
                }
            }).start();*/
            //audioTrack.play();
        }
    }

    public static void stopReceiving(){
        Log.d(TAG,"stopReceiving");
        receiving=false;
        /*if(audioTrack!=null) {
            audioTrack.stop();
            audioTrack=null;
        }*/
        if(activity!=null) {
            activity.finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
        //finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        stopReceiving();
        //MainActivity.stopSharing();
        //GetClientService.stopReceiving();
    }

    public static boolean isReceiving() {
        return receiving;
    }
}