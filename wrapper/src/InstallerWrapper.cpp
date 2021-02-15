#include <iostream>
#include <exception>
#include <shlobj.h>

#include "InstallerWrapper.h"
#include "../fabric-installer.h"
#include "../third-party/tinyfiledialogs/tinyfiledialogs.h"

int InstallerWrapper::boostrap() {
    std::filesystem::path javaPath;
    std::cout << "Bootstrapping Fabric Installer" << std::endl;

    try {
        javaPath = getJavaPath();
    } catch (std::exception &e) {
        tinyfd_messageBox("Fabric Installer", "Failed to find a valid java installation.", "ok", "error", 0);
        return -1;
    }
    std::cout << javaPath << std::endl;

    int code = runInstaller(javaPath);

    if (code != 0) {
        tinyfd_messageBox("Fabric Installer", "Failed to run installer.", "ok", "error", 0);
        return code;
    }

    return code;
}

int InstallerWrapper::runInstaller(std::filesystem::path& javaBinaryPath) {
    FILE *outFile;

    // TODO make this a temp file.
    int r = fopen_s(&outFile, "installer.jar", "wb");
    if (r != 0) {
        throw std::runtime_error("Failed to create temporary installer.jar file");
    }

    std::fwrite(INSTALLER_BIN, 1, INSTALLER_BIN_SIZE, outFile);
    fclose(outFile);

    std::string cmd = "\"" + javaBinaryPath.string() + "\" -jar installer.jar";

    return system(cmd.c_str());
}

std::filesystem::path InstallerWrapper::getJavaPath() {
    std::filesystem::path progFilesDir = getProgramFilesDirectory();
    std::filesystem::path minecraftDir = progFilesDir.append("Minecraft");

    if (std::filesystem::exists(minecraftDir)) {
        try {
            return getMinecraftJava(minecraftDir);
        } catch (std::exception &ignored) {
        }
    }

    throw std::runtime_error("Failed to find any valid java installation");
}

std::filesystem::path InstallerWrapper::getProgramFilesDirectory() {
    TCHAR progFiles[MAX_PATH];

    // Find C:\Program Files (x86)
    if (SUCCEEDED(SHGetFolderPath(NULL,
                                  CSIDL_PROGRAM_FILESX86,
                                  NULL,
                                  0,
                                  progFiles))) {
        return progFiles;
    }

    throw std::runtime_error("Failed to find program files");
}

std::filesystem::path InstallerWrapper::getMinecraftJava(std::filesystem::path &installationDir) {
    std::filesystem::path runtimeDir = installationDir.append("runtime");

    if (!std::filesystem::exists(runtimeDir)) {
        throw std::runtime_error("Could not find runtime directory");
    }

    // TODO there will be more, this is all I had on my pc
    // TODO the jre-legacy is a new format, we might want to do a bit of a recursive search here
    // TODO might want to ensure we use the correct arch.
    std::string searchDirectories[2] = {"jre-x64", "jre-legacy/windows-x64"};

    for (std::string searchPath : searchDirectories) {
        std::filesystem::path searchDir = runtimeDir.append(searchPath);
        if (!std::filesystem::exists(searchDir)) {
            continue;
        }


        // TODO taking the first version might not always be the best
        std::filesystem::path javaBinary;
        if (isValidJavaInstallation(searchDir, javaBinary)) {
            return javaBinary;
        }
    }

    throw std::runtime_error("Could not find a minecraft provided java installation");
}

bool InstallerWrapper::isValidJavaInstallation(std::filesystem::path &path, std::filesystem::path &javaBinaryPath) {
    std::filesystem::path javaBinary = path.append("bin").append("javaw.exe");

    if (!std::filesystem::exists(javaBinary)) {
        return false;
    }

    std::cout << javaBinary << std::endl;


    // TODO do we want to check the java version here, prob a good idea to ensure we have a good chance at finding a serviceable java installation
    javaBinaryPath = javaBinary;
    return true;
}
