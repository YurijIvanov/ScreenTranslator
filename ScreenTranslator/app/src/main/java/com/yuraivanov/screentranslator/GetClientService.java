package com.yuraivanov.screentranslator;

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
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GetClientService extends Service {
    private static ServerSocket videoServer,audioServer, commandServer;
    private static Socket videoClient, audioClient, commandClient;
    private static ExecutorService executorService;
    private static Future<?> videoThread, audioThread, commandThread;
    private final static String TAG = "GetClientService";
    private static Button button;
    private static boolean isUI=false;
    public static boolean isReceiving=false;
    private static boolean kg,kr,stop;
    private static Handler handler;

    public GetClientService(){}
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private Socket createSocket(ServerSocket serverSocket){
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            //Log.e(TAG, "socket connection error " + e.getMessage());
        }
        return null;
    }
    private ServerSocket createServer(int port){
        try {
            return new ServerSocket(port, 0, InetAddress.getByName(DeviceInfo.getIp()));
        } catch (IOException e) {
            Log.e(TAG, "commandServer create " + e.getMessage());
        }
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        stop=true;
        kg=true;
        kr=true;
        handler=new Handler(Looper.getMainLooper());
        //setUI(objects);
        isReceiving=true;
        executorService= Executors.newFixedThreadPool(3);
        commandServer = createServer(DeviceInfo.getCommandPort());
        videoServer = createServer(DeviceInfo.getVideoPort());
        audioServer = createServer(DeviceInfo.getAudioPort());
        if (commandServer == null || videoServer == null || audioServer == null) {
            stopSelf();
        } else {
            commandThread = executorService.submit(()->{
                InputStream input = null;
                BufferedReader buf=null;
                while (isReceiving){
                    if(commandClient==null){
                        commandClient=createSocket(commandServer);
                    }else if(!commandClient.isConnected()) {
                        try {
                            commandClient.close();
                            Log.d(TAG,"commandClient close");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        commandClient=null;
                        changeUi(false);
                    }else{
                        if(input==null) {
                            try {
                                input = commandClient.getInputStream();
                            } catch (IOException e) {
                                Log.e(TAG, "commandInputStream create " + e.getMessage());
                            }
                        }
                        if(input!=null){
                            if (buf == null) {
                                buf = new BufferedReader(new InputStreamReader(input));
                            }
                            String command = null;
                            try {
                                command = buf.readLine();
                            } catch (IOException e) {
                                Log.e(TAG, "Command read error:" + e.getMessage());
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
            });
            videoThread=executorService.submit(() -> {
                while (isReceiving){
                    if (videoClient == null) {
                        videoClient = createSocket(videoServer);
                        if (videoClient!=null) {
                            Log.d(TAG, "videoClient set");
                            changeUi(true);
                        }
                    }else if(!videoClient.isConnected()){
                        try {
                            videoClient.close();
                            Log.d(TAG,"videoClient close");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        videoClient=null;
                        changeUi(false);
                    }
                }
            });
            audioThread=executorService.submit(() -> {
                while (isReceiving){
                    if (audioClient == null) {
                        audioClient = createSocket(audioServer);
                        if(audioClient!=null) {
                            Log.d(TAG, "audioClient set");
                            changeUi(true);
                        }
                    }else if(!audioClient.isConnected()){
                        try {
                            audioClient.close();
                            Log.d(TAG,"audioClient close");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        audioClient=null;
                        changeUi(false);
                    }
                }
            });
        }
        if(!isReceiving){
            Log.e(TAG,"stopSelf");
            stopSelf();
        }
        return START_STICKY;
    }
    private void changeUi(boolean bol) {
        if (isUI) {
            Log.d(TAG,"isUI");
            if (bol && videoClient != null && audioClient != null && videoClient.isConnected() && audioClient.isConnected()) {
                if (kg) {
                    //activity.runOnUiThread(() ->{
                    handler.post(() -> {
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
        if(!bol){
            SurfaceActivity.stopReceiving();
        }
    }
    public static void setButton(Button button) {
        GetClientService.button = button;
    }

    public static void stopServer(){
        Log.e(TAG,"stopStreaming");
        isReceiving=false;
        if(commandThread!=null) {
            commandThread.cancel(true);
            commandThread=null;
        }
        if(commandClient!=null) {
            if (!commandClient.isClosed() && commandClient.isConnected()){
                if (stop) {
                    Thread t = new Thread(() -> {
                        BufferedWriter buf;
                        try {
                            buf = new BufferedWriter(new OutputStreamWriter(commandClient.getOutputStream()));
                            Log.d(TAG, "BufferedWriter created");
                            try {
                                buf.write("ScreenTranslator -stop");
                            } catch (IOException e) {
                                Log.e(TAG, "BufferedWriter write error" + e.getMessage());
                            }
                            try {
                                buf.close();
                            } catch (IOException e) {
                                Log.e(TAG, "BufferedWriter close error" + e.getMessage());
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "BufferedWriter create error" + e.getMessage());
                        }
                    });
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "t.join error" + e.getMessage());
                    }
                }
            }
            try {
                commandClient.close();
            } catch (IOException e) {
                Log.e(TAG,"commandClient close error" + e.getMessage());
            }
            commandClient=null;
        }
        if(commandServer!=null){
            try {
                commandServer.close();
            } catch (IOException e) {
                Log.e(TAG,"commandServer close error" + e.getMessage());
            }
            commandServer=null;
        }
        if(videoThread!=null) {
            videoThread.cancel(true);
            videoThread=null;
        }
        if(videoClient!=null){
            try {
                videoClient.close();
            } catch (IOException e) {
                Log.e(TAG,"videoClient close error" + e.getMessage());
            }
            videoClient=null;
        }

        if(videoServer!=null){
            try {
                videoServer.close();
            } catch (IOException e) {
                Log.e(TAG,"videoServer close error" + e.getMessage());
            }
            videoServer=null;
        }
        if(audioThread!=null) {
            audioThread.cancel(true);
            audioThread=null;
        }
        if(audioClient!=null){
            try {
                audioClient.close();
            } catch (IOException e) {
                Log.e(TAG,"audioClient close error" + e.getMessage());
            }
            audioClient=null;
        }
        if(audioServer!=null){
            try {
                audioServer.close();
            } catch (IOException e) {
                Log.e(TAG,"videoServer close error" + e.getMessage());
            }
            audioServer=null;
        }
        if(executorService!=null) {
            executorService.shutdown();
            executorService = null;
        }
        SurfaceActivity.stopReceiving();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        //stop=false;
        stopServer();
    }
    public static InputStream getCommandInput(){
        try {
            return commandClient.getInputStream();
        } catch (IOException e) {
            Log.e(TAG,"getVideoInput error "+e.getMessage());
            if(commandClient==null || !commandClient.isConnected()){
                SurfaceActivity.stopReceiving();
            }
        }
        return null;
    }
    public static InputStream getVideoInput(){
        try {
            return videoClient.getInputStream();
        } catch (IOException e) {
            Log.e(TAG,"getVideoInput error "+e.getMessage());
            if(videoClient==null || !videoClient.isConnected()){
                SurfaceActivity.stopReceiving();
            }
        }
        return null;
    }
    public static InputStream getAudioInput(){
        try {
            return audioClient.getInputStream();
        } catch (IOException e) {
            Log.e(TAG,"getAudioInput error "+e.getMessage());
            if(audioClient==null || !audioClient.isConnected()){
                SurfaceActivity.stopReceiving();
            }
        }
        return null;
    }

    public static Socket getVideoClient() {
        return videoClient;
    }
}