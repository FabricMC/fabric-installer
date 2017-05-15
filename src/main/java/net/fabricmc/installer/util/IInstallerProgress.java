package net.fabricmc.installer.util;

public interface IInstallerProgress {

	void updateProgress(String text, int percentage);

	void error(String error);

}
