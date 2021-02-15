#ifndef InstallerWrapper_H
#define InstallerWrapper_H

#include <filesystem>

class InstallerWrapper {
public:
    int boostrap();

private:
    int runInstaller(std::filesystem::path& javaBinaryPath);
    std::filesystem::path getJavaPath();
    std::filesystem::path getProgramFilesDirectory();
    std::filesystem::path getMinecraftJava(std::filesystem::path& installationDir);
    bool isValidJavaInstallation(std::filesystem::path& path, std::filesystem::path& javaBinaryPath);
};


#endif //InstallerWrapper_H
