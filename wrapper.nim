import os
import osproc
import streams
import strformat
import dialogs
import sequtils
import strutils

# Statically includes the installer binary
const installerBinary: seq[char] = toSeq(staticRead("/build/libs/fabric-installer-0.6.1.jar"))

type JavaException* = IOError

proc javaEnvPath(): string =
    if not os.existsEnv("JAVA_HOME"):
        return ""

    return os.getEnv("JAVA_HOME") & "javaw.exe"

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
        "C:\\Program Files (x86)\\Minecraft\\runtime\\jre-legacy\\windows-x64\\jre-legacy\\bin\\javaw.exe", # New location for mojang's "legacy" (8) installation of java
        "C:\\Program Files (x86)\\Minecraft\\runtime\\jre-x64\\bin\\javaw.exe",                             # The old default jre included with mc
        javaEnvPath(),                                                                                      # JAVA_HOME environment varible
        "javaw",                                                                                            # Java on the path
        ]

    for path in JREPaths:
        if isValidJavaPath(path):
            return path

    raise JavaException.newException("Failed to find a valid installation of java")

proc toString(str: seq[char]): string =
  result = newStringOfCap(len(str))
  for ch in str:
    add(result, ch)

proc runInstaller(path: string): void =
    let tempInstallerPath = os.getTempDir() & "fabric-installer.jar"
    echo "Writing fabric installer jar to " & tempInstallerPath
    writeFile(tempInstallerPath, toString(installerBinary))

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
        dialogs.error(nil, "The Fabric installer could not find java installed on your computer. Please visit the fabric wiki at https://fabricmc.net/wiki/ for help installing java.")
        system.quit(-1)

    runInstaller(path)
