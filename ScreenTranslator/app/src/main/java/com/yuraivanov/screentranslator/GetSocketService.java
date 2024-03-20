package com.yuraivanov.screentranslator;


import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GetSocketService extends Service {
    private static ExecutorService executorService;
    private static Future<?> videoThread, audioThread, commandTread;
    private static Socket videoSocket, audioSocket, commandSocket;
    private static ProgressBar progressBar;
    private static ImageView imageView;
    private static Button button;
    private static final String Tag = "GetSocketService";
    public static boolean isRecording=false;
    private static boolean isUI=false;
    private static boolean kg=true, kr=true, stop=true;
    private static Object[] objects;
    private static Handler handler;

    public GetSocketService(){

    }

    public static Socket getVideoSocket() {
        return videoSocket;
    }

    /*private void setUI(Object[] objects){
        //this.progressBar = (ProgressBar) objects[0];
        //this.imageView = (ImageView) objects[1];
        this.button = (Button) objects[2];
        isUI=true;
    }*/

    public static void setButton(Button button) {
        GetSocketService.button = button;
        isUI=true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Socket createSocket(int port){
        try {
            return new Socket(DeviceInfo.getIpJoin(),port);
        } catch (IOException ignore) {
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Tag, "onStartCommand");
        //Looper.prepare();
        handler=new Handler(Looper.getMainLooper());
        /*new Thread(()->{
            while (stop){
                if(objects!=null){
                    setUI(objects);
                    break;
                }
            }
        },"getUI").start();*/
        //setUI(objects);
        isRecording=true;
        executorService= Executors.newFixedThreadPool(3);
        commandTread=executorService.submit(()->{
            InputStream input = null;
            BufferedReader buf=null;
            while (isRecording){
                if(commandSocket==null){
                    commandSocket=createSocket(DeviceInfo.getCommandPort());
                }else{
                    if(input==null) {
                        try {
                            input = commandSocket.getInputStream();
                        } catch (IOException e) {
                            Log.e(Tag, "commandInputStream create " + e.getMessage());
                        }
                    }else{
                        if(buf==null) {
                            buf = new BufferedReader(new InputStreamReader(input));
                        }else {
                            String command = null;
                            try {
                                command = buf.readLine();
                            } catch (IOException e) {
                                Log.e(Tag, "Command read error:" + e.getMessage());
                            }
                            if (command != null && command.equals("ScreenTranslator -stop")) {
                                try {
                                    buf.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                try {
                                    input.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                changeUi(false);
                                stop=false;
                                stopSelf();
                            }
                        }
                    }
                }
            }
            Log.d(Tag,"commandTread end");
        });
        videoThread = executorService.submit(() -> {
            while (isRecording) {
                if (videoSocket == null) {
                    videoSocket=createSocket(DeviceInfo.getVideoPort());
                    if(videoSocket!=null) {
                        changeUi(true);
                    }
                }else{
                    if(videoSocket.isClosed()||!videoSocket.isConnected()){
                        changeUi(false);
                    }
                }
            }
            Log.d(Tag,"videoThread end");
        });
        audioThread = executorService.submit(() -> {
            while (isRecording) {
                if (audioSocket == null) {
                    audioSocket=createSocket(DeviceInfo.getAudioPort());
                    if(audioSocket!=null) {
                        changeUi(true);
                    }
                }else{
                    if(audioSocket.isClosed()||!audioSocket.isConnected()){
                        changeUi(false);
                    }
                }
            }
            Log.d(Tag,"audioThread end");
        });
        if(!isRecording) {
            Log.e(Tag,"stopSelf");
            stopSelf();
        }
        return START_STICKY;
    }
    private static void changeUi(boolean bol){
        if (isUI) {
            /*if(!bol){
                Log.d(Tag,"not bol");
            }*/
            if (bol && videoSocket != null && audioSocket != null && (videoSocket.isConnected() || !videoSocket.isClosed()) && (audioSocket.isConnected() || !audioSocket.isClosed())) {
                if (kg) {
                    //activity.runOnUiThread(() ->{
                    handler.post(()->{
                        Log.d(Tag,"post green");
                        /*progressBar.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);*/
                        button.setVisibility(View.VISIBLE);
                        //imageView.setImageResource(R.drawable.ic_green);
                    });
                    //});
                    kg=false;
                    kr=true;
                }
            }else{
                if(kr){
                    handler.post(() -> {
                        Log.d(Tag, "post red");
                        /*progressBar.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);*/
                        button.setVisibility(View.INVISIBLE);
                        //imageView.setImageResource(R.drawable.ic_red);
                    });
                    kr=false;
                    kg=true;
                }
            }
        }
    }
    public static void setObjects(Object[] objects){
        GetSocketService.objects=objects;
    }
    @Override
    public void onDestroy() {
        Log.d(Tag,"onDestroy");
        super.onDestroy();
        //stop=false;
        stopSocket();
    }
    public static void stopSocket(){
        Log.e(Tag,"stopSocket");
        isRecording=false;
        if(commandTread!=null) {
            commandTread.cancel(true);
            commandTread=null;
        }
        if(commandSocket!=null){
            if(stop && commandSocket.isConnected()) {
                Thread t = new Thread(() -> {
                    BufferedWriter buf = null;
                    try {
                        buf = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));
                        Log.d(Tag, "BufferedWriter created");
                        try {
                            buf.write("ScreenTranslator -stop");
                            buf.flush();
                        } catch (IOException e) {
                            Log.e(Tag, "BufferedWriter write error" + e.getMessage());
                        }
                        try {
                            buf.close();
                        } catch (IOException e) {
                            Log.e(Tag, "BufferedWriter close error" + e.getMessage());
                        }
                    } catch (IOException e) {
                        Log.e(Tag, "BufferedWriter create error" + e.getMessage());
                    }
                });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Log.e(Tag, "t.join error" + e.getMessage());
                }
            }
            try {
                commandSocket.close();
            } catch (IOException e) {
                Log.e(Tag,"commandSocket close error" + e.getMessage());
            }
            commandSocket=null;
        }
        if(videoThread!=null) {
            videoThread.cancel(true);
            videoThread=null;
        }
        if(videoSocket!=null){
            try {
                videoSocket.close();
            } catch (IOException e) {
                Log.e(Tag,"videoSocket close error" + e.getMessage());
            }
            videoSocket=null;
        }
        if(audioThread!=null) {
            audioThread.cancel(true);
            audioThread=null;
        }
        if(audioSocket!=null){
            try {
                audioSocket.close();
            } catch (IOException e) {
                Log.e(Tag,"audioSocket close error" + e.getMessage());
            }
            audioSocket=null;
        }
        if(executorService!=null) {
            executorService.shutdown();
            executorService = null;
        }
        changeUi(false);
        MainActivity.stopSharing();
        //RecordService.stopRecord();
        //GiveDialog.stopRecording();
    }
    public static OutputStream getCommandOutputStream(){
        try {
            return commandSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(Tag,"getOutputStream error "+e.getMessage());
            if(commandSocket==null || !commandSocket.isConnected()){
                MainActivity.stopSharing();
            }
        }
        return null;
    }
    public static OutputStream getVideoOutputStream(){
        try {
            return videoSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(Tag,"getOutputStream error "+e.getMessage());
            if(videoSocket==null || !videoSocket.isConnected() || videoSocket.isClosed()){
                MainActivity.stopSharing();
            }
        }
        return null;
    }
    public static OutputStream getAudioOutputStream(){
        try {
            return audioSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(Tag,"getOutputStream error "+e.getMessage());
            if(audioSocket==null || !audioSocket.isConnected()){
                MainActivity.stopSharing();
            }
        }
        return null;
    }
}