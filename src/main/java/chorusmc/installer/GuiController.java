package chorusmc.installer;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by Mark on 14/08/2016.
 */
public class GuiController {
    public RadioButton radioClient;
    public RadioButton radioServer;
    public ProgressBar progressBar;
    public TextField locationTextBox;

    public Button locationBrowseButton;
    public Button installButton;
    public ChoiceBox comboVersions;
    public Label statusText;
    public Label versionLabel;

    public void openLocationBrowser(Event event) {

    }

    public void installClicked(Event event) throws IOException {
        installButton.setVisible(false);

        radioClient.setDisable(true);
        radioServer.setDisable(true);
        locationTextBox.setDisable(true);
        locationBrowseButton.setDisable(true);
        comboVersions.setDisable(true);
        versionLabel.setDisable(true);

        statusText.setVisible(true);
        statusText.setText("Preparing to install");
        String str = (String) comboVersions.getValue();

        String[] split = str.split("-");
        if(radioClient.isSelected()){
            Optional<String> stringOptional = ClientInstaller.isValidInstallLocation(new File(locationTextBox.getText()), split[0]);
            if(stringOptional.isPresent()){
                statusText.setText(stringOptional.get());
                statusText.setTextFill(Color.RED);
            } else {
                ClientInstaller.install(new File(locationTextBox.getText()), str);
                statusText.setText("Done!");
            }
        } else {
            //DO things
        }


    }
}
