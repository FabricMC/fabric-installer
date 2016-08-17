package net.fabricmc.installer;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

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
		statusText.setVisible(true);
		statusText.setText("Preparing to install");
		String str = (String) comboVersions.getValue();

		String[] split = str.split("-");
		if (radioClient.isSelected()) {
			if (Main.isShiftDown) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Open Custom mod jar for installation");
				File file = fileChooser.showOpenDialog(Main.stage);
				if (file != null) {
					ClientInstaller.install(new File(locationTextBox.getText()), str, file);
				} else {
					statusText.setText("Select a custom jar for install");
				}
			} else {
				Optional<String> stringOptional = ClientInstaller.isValidInstallLocation(new File(locationTextBox.getText()), split[0]);
				if (stringOptional.isPresent()) {
					statusText.setText(stringOptional.get());
					statusText.setTextFill(Color.RED);
				} else {
					//TODO download jar from maven
					ClientInstaller.install(new File(locationTextBox.getText()), str, null);
					statusText.setText("Done!");
				}
			}

		} else {
			//DO things
		}

	}
}
