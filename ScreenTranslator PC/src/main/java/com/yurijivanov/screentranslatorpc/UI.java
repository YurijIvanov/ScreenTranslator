package com.yurijivanov.screentranslatorpc;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class UI {

    private static Stage stage;
    private static Scene mainScene, settingsScene, videoScene;

    public static void setStage(Stage stage) {
        UI.stage = stage;
    }

    public static void setMainScene(Scene mainScene) {
        UI.mainScene = mainScene;
    }

    public static void setSettingsScene(Scene settingsScene) {
        UI.settingsScene = settingsScene;
    }

    public static void setVideoScene(Scene videoScene) {
        UI.videoScene = videoScene;
    }

    public static void showMainScene(){
        stage.setScene(mainScene);
    }
    public static void showSettingsScene(){
        stage.setScene(settingsScene);
    }
    public static void showVideoScene(){
        stage.setScene(videoScene);
    }
    public static void show(){
        stage.show();
    }
    public static void close(){
        stage.close();
    }
}
