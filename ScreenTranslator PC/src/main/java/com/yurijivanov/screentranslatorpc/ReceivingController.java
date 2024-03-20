package com.yurijivanov.screentranslatorpc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReceivingController {
    private static final String Tag = "ReceivingController";
    private static ReceivingController instance;
    public static ReceivingController getInstance(){
        return instance;
    }
    private boolean initialize=false;

    @FXML
    private ImageView imageView;

    public void setImage(Image image) {
        imageView.setImage(image);
        image.cancel();
    }

    public static void setInstance(ReceivingController instance) {
        ReceivingController.instance = instance;
    }

    @FXML
    private void goMain(){
        stopReceiving();
        UI.showMainScene();
    }

    public void initialize(){
        initialize=true;
    }
    private static boolean receiving;
    //private static ExecutorService executorService;
    //private static Future<?> videoThread, audioThread;
    private InputStream videoInput;
    public void startReceiving(){
        System.out.println(Tag + " startReceiving");
        if(!instance.initialize){
            return;
        }
        receiving=true;
        //executorService= Executors.newFixedThreadPool(2);
        videoInput = SocketTask.getVideoInput();
        new Thread(() -> {
            if (videoInput == null) {
                SocketTask.stop();
                stopReceiving();
            } else {
                while (receiving) {
                    if(!MainApplication.isWork()||!MainApplication.isClientWork()){
                        stopReceiving();
                        return;
                    }
                    readObj();
                }
            }
        }).start();
    }
    private void readObj(){
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(videoInput);
        } catch (IOException e) {
            System.out.println(Tag + " readObj new ObjectInputStream " + e.getMessage());
            stopReceiving();
            return;
        }
        Object obj = null;
        try {
            obj = ois.readObject();
        } catch (IOException e) {
            System.out.println(Tag + " readObj ois.readObject IOException " + e.getMessage());
            if(e.getMessage().contains("Socket closed")||e.getMessage().contains("Broken pipe")){
                stopReceiving();
                return;
            }
            //stopServer();
        } catch (ClassNotFoundException e) {
            System.out.println(Tag + " readObj ois.readObject ClassNotFoundException " + e.getMessage());
        }
        if(obj!=null){
            byte[] imageBytes = (byte[]) obj;
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            Image image = new Image(bais);
            Platform.runLater(() -> setImage(image));
            try {
                bais.close();
            } catch (IOException e) {
                System.out.println(Tag + " readObj bais.close " + e.getMessage());
            }
        }
    }
    public static void stopReceiving() {
        System.out.println(Tag + " stopReceiving");
        receiving = false;
        //if(videoThread!=null) {
        //    videoThread.cancel(true);
        //    videoThread=null;
        //}
        //if(audioThread!=null){
        //    audioThread.cancel(true);
        //    audioThread=null;
        //}
        //if(executorService!=null){
        //    executorService.shutdown();
        //    executorService.close();
        //    executorService=null;
        //}
        if (MainApplication.isWork()) UI.showMainScene();
    }

    public static boolean isReceiving() {
        return receiving;
    }
}
