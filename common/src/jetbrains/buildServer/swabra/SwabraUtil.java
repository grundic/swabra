/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.swabra;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 17.04.2009
 * Time: 15:02:17
 */
public class SwabraUtil {
  public static final String MODE = "swabra.mode";
  public static final String VERBOSE = "swabra.verbose";
  public static final String LOCKING_PROCESS_DETECTION = "swabra.locking.processes";

  public static final String KILL = "swabra.kill";
  public static final String STRICT = "swabra.strict";

  public static final String AFTER_BUILD = "swabra.after.build";
  public static final String BEFORE_BUILD = "swabra.before.build";

  public static final String PROCESS_ANALIZER = "swabra.process.analizer";

  public static final String TRUE = "true";

  public static final Map<String, String> SWABRA_MODES = new HashMap<String, String>();

  static {
    SWABRA_MODES.put(AFTER_BUILD, "After build");
    SWABRA_MODES.put(BEFORE_BUILD, "Before next build");
  }

  public static boolean isSwabraEnabled(@NotNull final Map<String, String> params) {
    return params.containsKey(MODE);
  }

  public static String getSwabraMode(@NotNull final Map<String, String> params) {
    return params.get(MODE);
  }

  public static void enableSwabra(@NotNull final Map<String, String> params, @NotNull String mode) {
    params.put(MODE, mode);
  }

  public static boolean isVerbose(@NotNull final Map<String, String> params) {
    return params.containsKey(VERBOSE) && isSwabraEnabled(params);
  }

  public static boolean isKill(@NotNull final Map<String, String> params) {
    return params.containsKey(KILL) && isSwabraEnabled(params);
  }

  public static boolean isStrict(@NotNull final Map<String, String> params) {
    return params.containsKey(STRICT) && isSwabraEnabled(params);
  }

  public static boolean isLockingProcessesDetection(@NotNull final Map<String, String> params) {
    return SystemInfo.isWindows && params.containsKey(LOCKING_PROCESS_DETECTION);
  }

  public static String getHandlePath(@NotNull final Map<String, String> runParams) {
    return runParams.get(PROCESS_ANALIZER);
  }
}
