import AppKit
import Jni

let minecraftLauncherBundleIdentifier = "com.mojang.minecraftlauncher"

@_cdecl("Java_net_fabricmc_installer_launcher_MojangLauncherHelper_isMojangLauncherOpen")
public func isMojangLauncherOpen(jni: UnsafeMutablePointer<JNIEnv>, clazz: jclass!) -> jboolean {
  let isMinecraftLauncherRunning = NSWorkspace.shared.runningApplications
    .contains { $0.bundleIdentifier == minecraftLauncherBundleIdentifier }

  return jboolean(isMinecraftLauncherRunning ? JNI_TRUE : JNI_FALSE)
}
