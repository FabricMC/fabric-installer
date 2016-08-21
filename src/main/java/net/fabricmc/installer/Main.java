package net.fabricmc.installer;

import cuchaz.enigma.throwables.MappingParseException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import net.fabricmc.installer.utill.VersionInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Created by Mark on 14/08/2016.
 */
public class Main extends Application {

	private Scene scene;
	public static Stage stage;

	public static WebEngine webEngine;

	private static String errorMessage;

	public static void main(String[] args) throws IOException {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Main.stage = stage;
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

		VersionInfo.load();
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
		stage.setTitle("Fabric Installer");

		stage.getIcons().add(new Image("icon.png"));

		scene = new Scene(root, 700, 420);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();

		//Removes scroll bars on the webView
		controller.webView.getEngine().setUserStyleSheetLocation(classLoader.getResource("webView.css").toExternalForm());
		webEngine = controller.webView.getEngine();
		webEngine.setJavaScriptEnabled(true);
		webEngine.load(classLoader
			.getResource("./html/index.html")
			.toExternalForm());

		webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == Worker.State.SUCCEEDED) {
				String url = controller.webView.getEngine().getLocation();
				if (url.endsWith("index.html") || url.endsWith("server.html")) {
					stage.setHeight(420);
					for (String version : VersionInfo.versions) {
						webEngine.executeScript("var select, option; "
							+ "select = document.getElementById( 'versions' );"
							+ "option = document.createElement( 'option' );"
							+ "option.value = option.text = '" + version + "';"
							+ "select.add( option );");
					}
				} else if (url.endsWith("error.html")) {
					webEngine.executeScript("document.getElementById('error').innerHTML = '" + errorMessage + "';");
				}
			}

		});

		webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.endsWith("installing.html")) {
				String version = (String) webEngine.executeScript("$(\"#versions :selected\").text();");
				System.out.println("Installing: " + version);
				String[] split = version.split("-");
				new Thread(() -> {
					try {
						//Allows time for the page to load
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//TODO have a way to change the install location, might need some weird workaround, html not having a folder browser
					Optional<String> stringOptional = ClientInstaller.isValidInstallLocation(mcDefaultInstallLoc, split[0]);
					if (stringOptional.isPresent()) {
						error(stringOptional.get());
					} else {
						try {
							Main.setProgress("Installing: " + version, 0);
							stage.setHeight(300);
							ClientInstaller.install(mcDefaultInstallLoc, version, null);
						} catch (IOException | MappingParseException e) {
							e.printStackTrace();
							error(e.getLocalizedMessage());
						}

					}
				}).start();
			}

		});
	}

	public static void setProgress(String text, int percentage) {
		Platform.runLater(() -> {
			webEngine.executeScript("document.getElementById('status').innerHTML = '" + text + "';");
			webEngine.executeScript("$('#progress').css('width', " + percentage + "+'%').attr('aria-valuenow', " + percentage + ")");
		});
	}

	public static void done() {
		Platform.runLater(() -> {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				webEngine.load(classLoader
					.getResource("./html/done.html")
					.toExternalForm());
			}
		);
	}

	public static void error(String text) {
		stage.setHeight(310);
		Platform.runLater(() -> {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			webEngine.load(classLoader
				.getResource("./html/error.html")
				.toExternalForm());

			errorMessage = text;
		});
	}

}
