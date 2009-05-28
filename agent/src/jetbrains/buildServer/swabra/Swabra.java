/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/**
 * User: vbedrosova
 * Date: 14.04.2009
 * Time: 14:10:58
 */
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import static jetbrains.buildServer.swabra.SwabraUtil.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.intellij.openapi.util.io.FileUtil;


public class Swabra extends AgentLifeCycleAdapter {
  public static final Logger LOGGER = Logger.getLogger(Swabra.class);

  private Map<File, FileInfo> myFiles = new HashMap<File, FileInfo>();
  private BuildProgressLogger myLogger;
  private File myCheckoutDir;
  private String myMode;
  private boolean myVerbose;

  private final List<File> myAppeared = new ArrayList<File>();
  private final List<File> myModified = new ArrayList<File>();


  public Swabra(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
    agentDispatcher.addListener(this);
  }

  private static boolean needFullCleanup(final String prevMode) {
    return !isEnabled(prevMode);
  }

  private static boolean isEnabled(final String currMode) {
    return BEFORE_BUILD.equals(currMode) || AFTER_BUILD.equals(currMode);
  }
  public void buildStarted(@NotNull final AgentRunningBuild runningBuild) {
    final Map<String, String> runnerParams = runningBuild.getRunnerParameters();
    myLogger = runningBuild.getBuildLogger();
    myCheckoutDir = runningBuild.getCheckoutDirectory();
    myVerbose = isVerbose(runnerParams);
    final String mode = getSwabraMode(runnerParams);
    try {
      if (!isEnabled(mode)) {
        if (myFiles.size() > 0) {
          myFiles.clear();
        }
        return;
      }
      if (runningBuild.isCleanBuild()) {
        return;
      }
      if (needFullCleanup(myMode)) {
        // TODO: may be ask for clean build
        if (!FileUtil.delete(myCheckoutDir)) {
          warning("Unable to remove checkout directory on swabra work start");
        }
        return;
      }
      if (BEFORE_BUILD.equals(mode) && !AFTER_BUILD.equals(myMode)) {
        message("Previous build garbage cleanup is performed before build");
        collectGarbage(myVerbose);
      } else if (AFTER_BUILD.equals(mode) && BEFORE_BUILD.equals(myMode)) {
        // mode setting changed from "before build" to "after build"
        collectGarbage(false);
      }
    } finally {
      myMode = mode;
    }
  }

  public void buildFinished(@NotNull final BuildFinishedStatus buildStatus) {
    if (AFTER_BUILD.equals(myMode)) {
      message("Build garbage cleanup is performed after build");
      collectGarbage(myVerbose);
    }
  }

  private void collectGarbage(boolean verbose) {
    if (myCheckoutDir == null || !myCheckoutDir.isDirectory()) return;
    collectGarbage(myCheckoutDir);
    String target = null;
    if (verbose) {
      logTotals(target);
    }
    myAppeared.clear();
    myModified.clear();
    myFiles.clear();
  }

  private void logTotals(String target) {
    if (myAppeared.size() > 0) {
      target = "Build garbage";
      myLogger.targetStarted(target);
      for (File file : myAppeared) {
        message(file.getAbsolutePath());
      }
      myLogger.targetFinished(target);
    }

    if (myModified.size() > 0) {
      target = "Modified";
      myLogger.targetStarted(target);
      for (File file : myModified) {
        message(file.getAbsolutePath());
      }
      myLogger.targetFinished(target);
    }
    if (target == null) {
      message("No garbage or modified files detected");
    }
  }

  private void collectGarbage(@NotNull final File dir) {
    final File[] files = dir.listFiles();
    if (files == null || files.length == 0) return;
    for (File file : files) {
      final FileInfo info = myFiles.get(file);
      if (info == null) {
        System.out.println(file.getAbsolutePath() + " was added");
        myAppeared.add(file);
        if (file.isDirectory()) {
          //all directory content is supposed to be garbage
          collectGarbage(file);
        }
        if (!file.delete()) {
          warning("Unable to delete previous build garbage " + file.getAbsolutePath());
        }
      } else if ((file.lastModified() != info.getLastModified()) ||
                  file.length() != info.getLength()) {  //TODO: may be some other checking such as size

        System.out.println(file.getAbsolutePath() + " was modified");
        System.out.println("prevLastMod: " + file.lastModified() + ", currLastmod: " + info.getLastModified());
        System.out.println("prevLength: " + file.length() + ", currLength: " + info.getLength());
        myModified.add(file);
        if (file.isDirectory()) {
          //directory's content is supposed to be modified
          collectGarbage(file);
        }
      }
    }
  }

  public void beforeRunnerStart(@NotNull final AgentRunningBuild runningBuild) {
    if (!isEnabled(myMode)) return;
    myFiles.clear();
    saveState(myCheckoutDir);
  }

  private void saveState(@NotNull final File dir) {
    final File[] files = dir.listFiles();
    if (files == null || files.length == 0) return;
    for (File file : files) {
        myFiles.put(file, new FileInfo(file));
      if (file.isDirectory()) {
        saveState(file);
      }
    }
  }

  private void message(@NotNull final String message) {
    System.out.println(message);
    LOGGER.debug(message);
    myLogger.message(message);
  }

  private void warning(@NotNull final String message) {
    System.out.println(message);
    LOGGER.debug(message);
    myLogger.warning(message);
  }

  private static final class FileInfo {
    private final long myLength;
    private final long myLastModified;

    public FileInfo(File f) {
      myLastModified = f.lastModified();
      myLength = f.length();
    }

    public long getLastModified() {
      return myLastModified;
    }

    public long getLength() {
      return myLength;
    }
  }
}
