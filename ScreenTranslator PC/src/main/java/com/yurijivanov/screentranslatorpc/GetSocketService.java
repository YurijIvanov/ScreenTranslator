package com.yurijivanov.screentranslatorpc;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GetSocketService {
    /*private static ExecutorService executorService;
    private static Future<?> videoThread, audioThread, commandTread;
    private static Socket videoSocket, audioSocket, commandSocket;
    private static final String Tag = "GetSocketService";
    private static boolean work=false;
    private static boolean stop=true;

    public void startSocket(){
        work=true;
        executorService= Executors.newFixedThreadPool(3);
        commandTread=executorService.submit(()->{
            InputStream input = null;
            BufferedReader buf=null;
            while (work){
                if(commandSocket==null){
                    commandSocket=createSocket(DeviceInfo.getCommandPort());
                }else{
                    if(input==null) {
                        try {
                            input = commandSocket.getInputStream();
                        } catch (IOException e) {
                            System.out.println(Tag + " commandInputStream create " + e.getMessage());
                        }
                    }else{
                        if(buf==null) {
                            buf = new BufferedReader(new InputStreamReader(input));
                        }else {
                            String command = null;
                            if(commandSocket.isConnected()) {
                                try {
                                    command = buf.readLine();
                                } catch (IOException e) {
                                    System.out.println(Tag + " Command read error:" + e.getMessage());
                                }
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
                                MainApplication.stopSharing();
                                changeUi(false);
                                stop=false;
                            }
                        }
                    }
                }
            }
            System.out.println(Tag + " commandTread end");
        });
        videoThread = executorService.submit(() -> {
            while (work) {
                if (videoSocket == null) {
                    videoSocket=createSocket(DeviceInfo.getVideoPort());
                    if(videoSocket!=null) {
                        changeUi(true);
                    }
                }else{
                    if(videoSocket.isClosed()||!videoSocket.isConnected()){
                        MainApplication.stopSharing();
                        changeUi(false);
                    }
                }
            }
            System.out.println(Tag + " videoThread end");
        });
        audioThread = executorService.submit(() -> {
            while (work) {
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
            System.out.println(Tag + " audioThread end");
        });
    }

    public static Socket getVideoSocket() {
        return videoSocket;
    }

    private static Socket createSocket(int port){
        try {
            return new Socket(DeviceInfo.getIpJoin(),port);
        } catch (IOException ignore) {
        }
        return null;
    }
    private static void changeUi(boolean bol){
        boolean vision = bol && videoSocket != null && audioSocket != null &&
                (videoSocket.isConnected() || !videoSocket.isClosed()) &&
                (audioSocket.isConnected() || !audioSocket.isClosed());
        if(MainController.isInitialize()) {
            Platform.runLater(() -> MainController.getInstance().setShareButtonVisible(vision));
        }
        if(!vision){
            MainApplication.stopSharing();
        }
    }
    public static void stopSocket(){
        System.out.println(Tag + " stopSocket");
        work=false;
        if(commandTread!=null) {
            commandTread.cancel(true);
            commandTread=null;
        }
        if(commandSocket!=null){
            if(stop && commandSocket.isConnected()) {
                Thread t = new Thread(() -> {
                    BufferedWriter buf;
                    try {
                        buf = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));
                        System.out.println(Tag + " BufferedWriter created");
                        try {
                            buf.write(Tag + " ScreenTranslator -stop");
                            buf.flush();
                        } catch (IOException e) {
                            System.out.println(Tag + " BufferedWriter write error" + e.getMessage());
                        }
                        try {
                            buf.close();
                        } catch (IOException e) {
                            System.out.println(Tag + " BufferedWriter close error" + e.getMessage());
                        }
                    } catch (IOException e) {
                        System.out.println(Tag + " BufferedWriter create error" + e.getMessage());
                    }
                });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    System.out.println(Tag + " t.join error" + e.getMessage());
                }
            }
            try {
                commandSocket.close();
            } catch (IOException e) {
                System.out.println(Tag + " commandSocket close error" + e.getMessage());
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
                System.out.println(Tag + " videoSocket close error" + e.getMessage());
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
                System.out.println(Tag + " audioSocket close error" + e.getMessage());
            }
            audioSocket=null;
        }
        if(executorService!=null) {
            executorService.shutdown();
            executorService = null;
        }
        changeUi(false);
    }

    public static OutputStream getCommandOutputStream(){
        try {
            return commandSocket.getOutputStream();
        } catch (IOException e) {
            System.out.println(Tag + " getOutputStream error "+e.getMessage());
            if(commandSocket==null || !commandSocket.isConnected()){
                stopSocket();
            }
        }
        return null;
    }
    public static OutputStream getVideoOutputStream(){
        try {
            return videoSocket.getOutputStream();
        } catch (IOException e) {
            System.out.println(Tag + " getOutputStream error "+e.getMessage());
            if(videoSocket==null || !videoSocket.isConnected()){
                stopSocket();
            }
        }
        return null;
    }
    public static OutputStream getAudioOutputStream(){
        try {
            return audioSocket.getOutputStream();
        } catch (IOException e) {
            System.out.println(Tag + " getOutputStream error "+e.getMessage());
            if(audioSocket==null || !audioSocket.isConnected()){
                stopSocket();
            }
        }
        return null;
    }
    public static boolean isWork() {
        return work;
    }*/
}