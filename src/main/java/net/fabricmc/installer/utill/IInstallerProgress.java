package net.fabricmc.installer.utill;

public interface IInstallerProgress {

	void updateProgress(String text, int percentage);

	void error(String error);

}
