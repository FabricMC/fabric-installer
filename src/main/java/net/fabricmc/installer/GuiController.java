package net.fabricmc.installer;

import cuchaz.enigma.throwables.MappingParseException;
import javafx.application.Platform;
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

	public GuiController() {

	}

	public void openLocationBrowser(Event event) {
	}

	public void installClicked(Event event) throws IOException, MappingParseException {
		statusText.setVisible(true);
		setText("Preparing to install");
		String str = (String) comboVersions.getValue();

		String[] split = str.split("-");
		if (radioClient.isSelected()) {
			if (Main.isShiftDown) {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Open Custom mod jar for installation");
				File file = fileChooser.showOpenDialog(Main.stage);
				if (file != null && file.getName().endsWith(".jar")) {
					new Thread(() -> {
						try {
							ClientInstaller.install(new File(locationTextBox.getText()), str, file, this);
						} catch (IOException | MappingParseException e) {
							e.printStackTrace();
							setText(e.getMessage());
						}
					}).start();
				} else {
					setText("Select a custom jar for install");
				}
			} else {
				Optional<String> stringOptional = ClientInstaller.isValidInstallLocation(new File(locationTextBox.getText()), split[0]);
				if (stringOptional.isPresent()) {
					setText(stringOptional.get());
					statusText.setTextFill(Color.RED);
				} else {
					new Thread(() -> {
						try {
							ClientInstaller.install(new File(locationTextBox.getText()), str, null, this);
						} catch (IOException | MappingParseException e) {
							e.printStackTrace();
							setText(e.getMessage());
						}
					}).start();
				}
			}

		} else {
			//DO things
		}

	}

	public void setText(String text) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				statusText.setText(text);
			}
		});
	}
}
