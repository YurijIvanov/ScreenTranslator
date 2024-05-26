package com.yurijivanov.screentranslatorpc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class MainApplication extends Application {
    private final static String Tag = "MainApplication";
    private static boolean sharing, work=true, socketWork=false, clientWork=false;
    public static boolean isWork() {
        return work;
    }
    public static boolean isSocketWork() {
        return socketWork;
    }
    public static void setSocketWork(boolean socketWork) {
        MainApplication.socketWork = socketWork;
    }
    public static boolean isClientWork() {
        return clientWork;
    }
    public static void setClientWork(boolean clientWork) {
        MainApplication.clientWork = clientWork;
    }
    public static boolean isSharing() {
        return sharing;
    }

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println(Tag + " start");
        work=true;
        stage.setTitle("ScreenTranslator");
        UI.setStage(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("view/main-view.fxml"));
        UI.setMainScene(new Scene(fxmlLoader.load()));
        MainController.setInstance(fxmlLoader.getController());
        fxmlLoader = new FXMLLoader(MainApplication.class.getResource("view/settings-view.fxml"));
        UI.setSettingsScene(new Scene(fxmlLoader.load()));
        fxmlLoader=new FXMLLoader(MainApplication.class.getResource("view/receiving-view.fxml"));
        UI.setVideoScene(new Scene(fxmlLoader.load()));
        ReceivingController.setInstance(fxmlLoader.getController());
        UI.showMainScene();
        UI.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println(Tag +" stop()");
        work=false;
        socketWork=false;
        clientWork=false;
        sharing=false;
        SocketTask.stop();
        ReceivingController.stopReceiving();
        stopSharing();
        super.stop();
    }
    public static void startSharing(int index){
        sharing=true;
        new Thread(() -> {
            Screen selectedScreen = Screen.getScreens().get(index);
            try {
                Robot robot = new Robot();
                OutputStream videoOutput = SocketTask.getVideoOutputStream();
                ObjectOutputStream outstream = null;
                if(videoOutput!=null) {
                    while (sharing && work) {
                        try {
                            outstream = new ObjectOutputStream(videoOutput);
                        } catch (IOException e) {
                            System.out.println(Tag + " startSharing new ObjectOutputStream " + e.getMessage());
                            stopSharing();
                            break;
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Rectangle2D rectangle = selectedScreen.getBounds();
                        BufferedImage img = robot.createScreenCapture(new java.awt.Rectangle(
                                (int) rectangle.getMinX(), (int) rectangle.getMinY(),
                                (int) rectangle.getWidth(), (int) rectangle.getHeight()));
                        try {
                            ImageIO.write(img, "jpg", baos);
                        } catch (IOException e) {
                            System.out.println(Tag + " startSharing ImageIO.write " + e.getMessage());
                            stopSharing();
                            break;
                        }
                        byte[] imageData = baos.toByteArray();
                        try {
                            outstream.writeObject(imageData);
                            outstream.flush();
                        } catch (IOException e) {
                            System.out.println(Tag + " startSharing outstream.writeObject/flush " + e.getMessage());
                            if (e.getMessage().contains("Socket closed") || e.getMessage().contains("Broken pipe")) {
                                stopSharing();
                                break;
                            }
                        }
                    }
                }
                if (outstream != null) {
                    try {
                        outstream.close();
                    } catch (IOException e) {
                        System.out.println(Tag + " startSharing outstream.close " + e.getMessage());
                    }
                }
            } catch (AWTException e) {
                System.out.println(Tag + " startSharing AWTException " + e.getMessage());
            }
            System.out.println(Tag + " sharing thread stop");
        }).start();
    }
    public static void stopSharing(){
        System.out.println(Tag + " stopSharing");
        sharing=false;
    }

    public static void main(String[] args) {
        System.out.println(Tag + " main()");
        launch();
        System.out.println(Tag + " main() -> launch()");
        System.exit(0);
    }


}