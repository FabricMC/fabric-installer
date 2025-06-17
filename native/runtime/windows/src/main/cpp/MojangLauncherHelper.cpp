/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "MojangLauncherHelper.h"

#include <Windows.h>

// Include after Windows.h
#include <wil/resource.h>

constexpr LPCWSTR MinecraftLauncherMutexName = L"MojangLauncher";

JNIEXPORT jboolean JNICALL
Java_net_fabricmc_installer_launcher_MojangLauncherHelper_isMojangLauncherOpen(
    JNIEnv *, jclass) {
  wil::unique_handle handle{::OpenMutexW(0, false, MinecraftLauncherMutexName)};

  if (::GetLastError() == ERROR_FILE_NOT_FOUND) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}