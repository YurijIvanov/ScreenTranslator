package com.yuraivanov.screentranslator;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketTask {
    private static final String Tag = "SocketTask";
    private static ServerSocket videoServer, /*audioServer,*/ commandServer;
    private static Socket videoSocket, /*audioSocket,*/ commandSocket;
    private static boolean stop,task;

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static ServerSocket createServer(int port){
        try {
            return new ServerSocket(port, 0, InetAddress.getByName(DeviceInfo.getIp()));
        } catch (IOException e) {
            Log.e(Tag,"commandServer create " + e.getMessage());
        }
        return null;
    }
    private static Socket createClient(ServerSocket serverSocket){
        try {
            return serverSocket.accept();
        } catch (IOException ignore) {
            //Log.e(Tag,""socket connection error " + e.getMessage());
        }
        return null;
    }
    private static Socket createSocket(int port){
        try {
            return new Socket(DeviceInfo.getIpJoin(),port);
        } catch (IOException ignore) {
        }
        return null;
    }
    public static void startServer(){
        Log.d(Tag,"startServer");
        task=true;
        stop=true;
        MainActivity.setClientWork(true);
        commandServer = createServer(DeviceInfo.getCommandPort());
        videoServer = createServer(DeviceInfo.getVideoPort());
        //audioServer = createServer(DeviceInfo.getAudioPort());
        if (commandServer == null || videoServer == null /*|| audioServer == null*/) {
            stop();
        } else {
            new Thread(()->{
                Log.d(Tag,"startServer commandSocket thread start");
                while(MainActivity.isWork()&&MainActivity.isClientWork()&&commandSocket==null){
                    commandSocket = createClient(commandServer);
                    if (commandSocket != null) {
                        MainActivity.setIpJoinEditText(commandSocket.getInetAddress().getHostAddress());
                        Log.d(Tag, "startServer commandSocket created");
                    }
                }
                Log.d(Tag,"startServer commandSocket thread end");
            }).start();
            new Thread(()->{
                Log.d(Tag,"startServer videoSocket thread start");
                while(MainActivity.isWork()&&MainActivity.isClientWork()&&videoSocket==null) {
                    videoSocket = createClient(videoServer);
                    if (videoSocket != null) {
                        Log.d(Tag, "startServer videoSocket created");
                    }
                    changeUi(true);
                }
                Log.d(Tag,"startServer videoSocket thread end");
            }).start();
            /*new Thread(()->{
                Log.d(Tag,"startServer audioSocket thread start");
                while(MainActivity.isWork()&&MainActivity.isClientWork()&&audioSocket==null){
                    audioSocket = createClient(audioServer);
                    if (audioSocket != null) {
                        Log.d(Tag, "startServer audioSocket created");
                    }
                    changeUi(true);
                }
                Log.d(Tag,"startServer audioSocket thread end");
            }).start();*/
            checkStop();
        }
    }

    public static void startSocket(){
        Log.d(Tag,"startSocket");
        task=false;
        stop=true;
        MainActivity.setSocketWork(true);
        new Thread(()->{
            Log.d(Tag,"startSocket commandSocket thread start");
            while(MainActivity.isWork()&&MainActivity.isSocketWork()&&commandSocket==null){
                commandSocket = createSocket(DeviceInfo.getCommandPort());
                if (commandSocket != null) {
                    Log.d(Tag, "startSocket commandSocket created");
                }
            }
            Log.d(Tag,"startSocket commandSocket thread end");
        }).start();
        new Thread(()->{
            Log.d(Tag,"startSocket videoSocket thread start");
            while(MainActivity.isWork()&&MainActivity.isSocketWork()&&videoSocket==null){
                videoSocket = createSocket(DeviceInfo.getVideoPort());
                if (videoSocket != null) {
                    Log.d(Tag, "startSocket videoSocket created");
                }
                changeUi(true);
            }
            Log.d(Tag,"startSocket videoSocket thread end");
        }).start();
        /*new Thread(()->{
            Log.d(Tag,"startSocket audioSocket thread start");
            while(MainActivity.isWork()&&MainActivity.isSocketWork()&&audioSocket==null){
                audioSocket = createSocket(DeviceInfo.getAudioPort());
                if (audioSocket != null) {
                    Log.d(Tag, "startSocket audioSocket created");
                }
                changeUi(true);
            }
            Log.d(Tag,"startSocket audioSocket thread end");
        }).start();*/
        checkStop();
    }
    private static void checkStop() {
        new Thread(()->{
            InputStream input;
            BufferedReader buf=null;
            while (MainActivity.isWork()&&(MainActivity.isClientWork()||MainActivity.isSocketWork())){
                if(commandSocket!=null){
                    input = getCommandInput();
                    if(input!=null) {
                        if(buf==null) {
                            buf = new BufferedReader(new InputStreamReader(input));
                        }
                        String command = null;
                        try {
                            command = buf.readLine();
                        } catch (IOException e) {
                            Log.e(Tag,"checkStop Command read " + task + " error: " + e.getMessage());
                            stop();
                        }
                        if (command != null && command.equals("ScreenTranslator -stop")) {
                            try {
                                buf.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            stop=false;
                            stop();
                        }
                    }else stop();
                }
                if(!MainActivity.isWork()){
                    break;
                }
            }
        }).start();
    }
    public static void stop(){
        Log.d(Tag,"stop");
        MainActivity.setClientWork(false);
        MainActivity.setSocketWork(false);
        if(commandSocket!=null){
            if(stop) {
                Thread t = new Thread(() -> {
                    OutputStream cos = getCommandOutputStream();
                    BufferedWriter buf;
                    if(cos !=null) {
                        buf = new BufferedWriter(new OutputStreamWriter(cos));
                        Log.d(Tag,"stop BufferedWriter created");
                        try {
                            buf.write(" ScreenTranslator -stop");
                        } catch (IOException e) {
                            Log.e(Tag,"BufferedWriter write error" + e.getMessage());
                        }
                        try {
                            buf.close();
                        } catch (IOException e) {
                            Log.e(Tag,"BufferedWriter close error" + e.getMessage());
                        }
                    }
                });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Log.e(Tag,"t.join error" + e.getMessage());
                }
            }
            try {
                commandSocket.close();
            } catch (IOException e) {
                Log.e(Tag,"commandSocket close error" + e.getMessage());
            }
            commandSocket=null;
        }
        if(commandServer!=null){
            try {
                commandServer.close();
            } catch (IOException e) {
                Log.e(Tag,"commandServer close error" + e.getMessage());
            }
            commandServer=null;
        }
        if(videoSocket!=null){
            try {
                videoSocket.close();
            } catch (IOException e) {
                Log.e(Tag,"videoSocket close error" + e.getMessage());
            }
            videoSocket=null;
        }
        if(videoServer!=null){
            try {
                videoServer.close();
            } catch (IOException e) {
                Log.e(Tag,"videoServer close error" + e.getMessage());
            }
            videoServer=null;
        }
        /*if(audioSocket!=null){
            try {
                audioSocket.close();
            } catch (IOException e) {
                Log.e(Tag,"audioSocket close error" + e.getMessage());
            }
            audioSocket=null;
        }
        if(audioServer!=null){
            try {
                audioServer.close();
            } catch (IOException e) {
                Log.e(Tag,"videoServer close error" + e.getMessage());
            }
            audioServer=null;
        }*/
        if(SurfaceActivity.isReceiving()){
            SurfaceActivity.stopReceiving();
        }
        changeUi(false);
    }
    public static OutputStream getCommandOutputStream(){
        if(commandSocket!=null && commandSocket.isConnected() && !commandSocket.isClosed()) {
            try {
                return commandSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(Tag,"getCommandOutputStream error " + e.getMessage());
                if (commandSocket == null || !commandSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    public static OutputStream getVideoOutputStream(){
        if(videoSocket!=null && videoSocket.isConnected() && !videoSocket.isClosed()) {
            try {
                return videoSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(Tag,"getVideoOutputStream error " + e.getMessage());
                if (videoSocket == null || !videoSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    /*public static OutputStream getAudioOutputStream(){
        if(audioSocket!=null && audioSocket.isConnected() && !audioSocket.isClosed()) {
            try {
                return audioSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(Tag,"getAudioOutputStream error " + e.getMessage());
                if (audioSocket == null || !audioSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }*/
    public static InputStream getCommandInput(){
        if(commandSocket!=null && commandSocket.isConnected() && !commandSocket.isClosed()) {
            try {
                return commandSocket.getInputStream();
            } catch (IOException e) {
                Log.e(Tag,"getCommandInput error " + e.getMessage());
                if (commandSocket == null || !commandSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    public static InputStream getVideoInput(){
        if(videoSocket!=null && videoSocket.isConnected() && !videoSocket.isClosed()) {
            try {
                return videoSocket.getInputStream();
            } catch (IOException e) {
                Log.e(Tag,"getVideoInput error " + e.getMessage());
                if (videoSocket == null || !videoSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    /*public static InputStream getAudioInput(){
        if(audioSocket!=null && audioSocket.isConnected() && !audioSocket.isClosed()) {
            try {
                return audioSocket.getInputStream();
            } catch (IOException e) {
                Log.e(Tag,"getAudioInput error " + e.getMessage());
                if (audioSocket == null || !audioSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }*/
    public static void changeUi(boolean bol) {
        boolean vision = bol && ((videoSocket!=null && videoSocket.isConnected()&& !videoSocket.isClosed()) /*&& (audioSocket!=null && audioSocket.isConnected()&& !audioSocket.isClosed())*/);
        handler.post(()->MainActivity.changeUi(vision,task));
        if(!vision){
            if(task) SurfaceActivity.stopReceiving();
            else MainActivity.stopSharing();
        }
    }
}
