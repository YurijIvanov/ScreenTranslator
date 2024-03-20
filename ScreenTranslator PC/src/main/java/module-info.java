module com.yurijivanov.screentranslatorpc {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.yurijivanov.screentranslatorpc to javafx.fxml;
    exports com.yurijivanov.screentranslatorpc;
}