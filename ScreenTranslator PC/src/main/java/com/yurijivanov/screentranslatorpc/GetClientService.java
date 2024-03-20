package com.yurijivanov.screentranslatorpc;

import javafx.application.Platform;
import javafx.scene.control.Button;

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

public class GetClientService {
    /*private static ServerSocket videoServer, audioServer, commandServer;
    private static Socket videoClient, audioClient, commandClient;
    private static ExecutorService executorService;
    private static Future<?> videoThread, audioThread, commandThread;
    private final static String Tag = "GetClientService";
    private static boolean work=false, stop;

    private static ServerSocket createServer(int port){
        try {
            return new ServerSocket(port, 0, InetAddress.getByName(DeviceInfo.getIp()));
        } catch (IOException e) {
            System.out.println(Tag + " commandServer create " + e.getMessage());
        }
        return null;
    }
    private static Socket createSocket(ServerSocket serverSocket){
        try {
            return serverSocket.accept();
        } catch (IOException ignore) {
            //System.out.println(Tag + " "socket connection error " + e.getMessage());
        }
        return null;
    }
    public static void startClient(){
        stop=true;
        work=true;
        executorService= Executors.newFixedThreadPool(3);
        commandServer = createServer(DeviceInfo.getCommandPort());
        videoServer = createServer(DeviceInfo.getVideoPort());
        audioServer = createServer(DeviceInfo.getAudioPort());
        if (commandServer == null || videoServer == null || audioServer == null) {
            stopClient();
        } else {
            commandThread = executorService.submit(()->{
                InputStream input = null;
                BufferedReader buf=null;
                while (work){
                    if(commandClient==null){
                        commandClient=createSocket(commandServer);
                    }else if(!commandClient.isConnected()) {
                        try {
                            commandClient.close();
                            System.out.println(Tag + " commandClient close");
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
                                System.out.println(Tag + " commandInputStream create " + e.getMessage());
                            }
                        }else {
                            if (buf == null) {
                                buf = new BufferedReader(new InputStreamReader(input));
                            } else{
                                String command = null;
                                try {
                                    command = buf.readLine();
                                } catch (IOException e) {
                                    System.out.println(Tag + " Command read error:" + e.getMessage());
                                    stopClient();
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
                                    stopClient();
                                }
                            }
                        }
                    }
                }
            });
            videoThread=executorService.submit(() -> {
                while (work){
                    if (videoClient == null) {
                        videoClient = createSocket(videoServer);
                        System.out.println(Tag + " videoClient set");
                        changeUi(true);
                    }else if(!videoClient.isConnected()){
                        try {
                            videoClient.close();
                            System.out.println(Tag + " videoClient close");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        videoClient=null;
                        changeUi(false);
                    }
                }
            });
            audioThread=executorService.submit(() -> {
                while (work){
                    if (audioClient == null) {
                        audioClient = createSocket(audioServer);
                        System.out.println(Tag + " audioClient set");
                        changeUi(true);
                    }else if(!audioClient.isConnected()){
                        try {
                            audioClient.close();
                            System.out.println(Tag + " audioClient close");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        audioClient=null;
                        changeUi(false);
                    }
                }
            });
        }
    }
    private static void changeUi(boolean bol) {
        boolean vision = bol && videoClient != null && audioClient != null && videoClient.isConnected() && audioClient.isConnected();
        if(MainController.isInitialize()) {
            Platform.runLater(() -> MainController.getInstance().setReceiveButtonVisible(vision));
        }
        if(!vision){
            MainApplication.stopReceiving();
        }
    }
    public static boolean isWork() {
        return work;
    }
    public static void stopClient(){
        System.out.println(Tag + " stopStreaming");
        work=false;
        if(commandThread!=null) {
            commandThread.cancel(true);
            commandThread=null;
        }
        if(commandClient!=null){
            if(stop) {
                Thread t = new Thread(() -> {
                    BufferedWriter buf;
                    try {
                        buf = new BufferedWriter(new OutputStreamWriter(commandClient.getOutputStream()));
                        System.out.println(Tag + " BufferedWriter created");
                        try {
                            buf.write("ScreenTranslator -stop");
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
                commandClient.close();
            } catch (IOException e) {
                System.out.println(Tag + " commandClient close error" + e.getMessage());
            }
            commandClient=null;
        }
        if(commandServer!=null){
            try {
                commandServer.close();
            } catch (IOException e) {
                System.out.println(Tag + " commandServer close error" + e.getMessage());
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
                System.out.println(Tag + " videoClient close error" + e.getMessage());
            }
            videoClient=null;
        }
        if(videoServer!=null){
            try {
                videoServer.close();
            } catch (IOException e) {
                System.out.println(Tag + " videoServer close error" + e.getMessage());
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
                System.out.println(Tag + " audioClient close error" + e.getMessage());
            }
            audioClient=null;
        }
        if(audioServer!=null){
            try {
                audioServer.close();
            } catch (IOException e) {
                System.out.println(Tag + " videoServer close error" + e.getMessage());
            }
            audioServer=null;
        }
        if(executorService!=null) {
            executorService.shutdown();
            executorService = null;
        }
        if(MainApplication.isReceiving()) {
            MainApplication.stopReceiving();
        }
    }

    public static InputStream getCommandInput(){
        try {
            return commandClient.getInputStream();
        } catch (IOException e) {
            System.out.println(Tag + " getVideoInput error "+e.getMessage());
            if(commandClient==null || !commandClient.isConnected()){
                MainApplication.stopReceiving();
            }
        }
        return null;
    }
    public static InputStream getVideoInput(){
        try {
            return videoClient.getInputStream();
        } catch (IOException e) {
            System.out.println(Tag + " getVideoInput error "+e.getMessage());
            if(videoClient==null || !videoClient.isConnected()){
                MainApplication.stopReceiving();
            }
        }
        return null;
    }
    public static InputStream getAudioInput(){
        try {
            return audioClient.getInputStream();
        } catch (IOException e) {
            System.out.println(Tag + " getAudioInput error "+e.getMessage());
            if(audioClient==null || !audioClient.isConnected()){
                MainApplication.stopReceiving();
            }
        }
        return null;
    }

    public static Socket getVideoClient() {
        return videoClient;
    }*/
}