import os
import osproc
import streams
import strformat
import dialogs

# Statically includes the installer binary
const installerBinary = staticRead("/build/libs/fabric-installer-0.7.0.jar")

type JavaException* = IOError

proc javaEnvPath(): string =
    if not os.existsEnv("JAVA_HOME"):
        return ""

    return os.getEnv("JAVA_HOME") & "java.exe"

proc isValidJavaPath(path: string): bool =
    try:
        let process = osproc.startProcess(
         command = path,
         args = ["-version"],
         options={poEchoCmd, poStdErrToStdOut}
        )

        close(process)
        return process.peekExitCode() == 0
    except Exception:
        return false

proc findJavaPath(): string =
    const JREPaths = [
        "C:\\Program Files (x86)\\Minecraft\\runtime\\jre-legacy\\windows-x64\\jre-legacy\\bin\\java.exe",  # New location for mojang's "legacy" (8) installation of java
        "C:\\Program Files (x86)\\Minecraft\\runtime\\jre-x64\\bin\\java.exe",                              # The old default jre included with mc
        javaEnvPath(),                                                                                      # JAVA_HOME environment varible
        "java",                                                                                             # Java on the path
        ]

    for path in JREPaths:
        if isValidJavaPath(path):
            return path

    raise JavaException.newException("Failed to find a valid installation of java")

proc runInstaller(path: string): void =
    let tempInstallerPath = os.getTempDir() & "fabric-installer.jar"
    echo "Writing fabric installer jar to " & tempInstallerPath
    writeFile(tempInstallerPath, installerBinary)

    let process = osproc.startProcess(
         command = path,
         args = ["-jar", tempInstallerPath],
         options={poEchoCmd, poStdErrToStdOut}
        )

    # Forward the log output
    let strm = osproc.outputStream(process)
    var line = ""
    while strm.readLine(line):
        echo line

    close(process)

    # Forward the exit code
    let exitCode = process.peekExitCode()
    echo &"Fabric installer exit code: {exitCode}"
    system.quit(exitCode)

when isMainModule:
    echo "Fabric installer bootstrap"

    when not defined windows:
        echo "Only windows is currently supported"
        system.quit(-1)

    var path = ""

    try:
        path = findJavaPath()
    except JavaException as e:
        echo e.msg
        dialogs.error(nil, "The Fabric installer could not find java installed on your computer.")
        system.quit(-1)

    runInstaller(path)
