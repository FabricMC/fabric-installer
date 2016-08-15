package chorusmc.installer;

import chorusmc.installer.utill.VersionInfo;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by Mark on 14/08/2016.
 */
public class Main extends Application {

    private Scene scene;

    public static void main(String[] args) throws IOException {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL fxmlUrl = classLoader.getResource("gui.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException("Could not find fxml file");
        }
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(fxmlUrl);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());

        Parent root = fxmlLoader.load(fxmlUrl.openStream());

        GuiController controller = fxmlLoader.getController();

        controller.radioServer.setDisable(true);
        controller.radioClient.setSelected(true);

        VersionInfo.load();

        controller.comboVersions.setItems(FXCollections.observableArrayList(VersionInfo.versions));
        controller.comboVersions.setValue(VersionInfo.latestVersion);

        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name").toLowerCase();
        File mcDefaultInstallLoc;
        File homeDir = new File(home);

        if (os.contains("win") && System.getenv("APPDATA") != null) {
            mcDefaultInstallLoc = new File(System.getenv("APPDATA"), ".minecraft");
        } else if (os.contains("mac")) {

            mcDefaultInstallLoc = new File(homeDir, "Library" + File.separator + "Application Support" + File.separator + "minecraft");
        } else {
            mcDefaultInstallLoc = new File(homeDir, ".minecraft");
        }
        controller.locationTextBox.setText(mcDefaultInstallLoc.getAbsolutePath());
        stage.setTitle("Chorus Installer");

        scene = new Scene(root, 352, 184);
        stage.setScene(scene);

        stage.show();
    }

}
