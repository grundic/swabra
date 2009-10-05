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
import java.util.*;


public final class Swabra extends AgentLifeCycleAdapter {
  public static final String WORK_DIR_PROP = "agent.work.dir";

  private SwabraLogger myLogger;
  private SmartDirectoryCleaner myDirectoryCleaner;

  private String myMode;
  private boolean myVerbose;
  private volatile Snapshot mySnapshot;

  private volatile Map<File, PreviousCleanupInfo> myPrevModes = new HashMap<File, PreviousCleanupInfo>();

  private static boolean isEnabled(final String currMode) {
    return BEFORE_BUILD.equals(currMode) || AFTER_BUILD.equals(currMode);
  }

  private static boolean needFullCleanup(final String prevMode) {
    return !isEnabled(prevMode);
  }

  public Swabra(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                @NotNull final SmartDirectoryCleaner directoryCleaner) {
    agentDispatcher.addListener(this);
    myDirectoryCleaner = directoryCleaner;
  }

  public void buildStarted(@NotNull final AgentRunningBuild runningBuild) {
    final Map<String, String> runnerParams = runningBuild.getRunnerParameters();
    myLogger = new SwabraLogger(runningBuild.getBuildLogger(), Logger.getLogger(Swabra.class));
    myVerbose = isVerbose(runnerParams);
    String mode = getSwabraMode(runnerParams);
    final File checkoutDir = runningBuild.getCheckoutDirectory();
    final PreviousCleanupInfo info = myPrevModes.get(checkoutDir);
    if (info != null) {
      final Thread t = info.getWorkingThread();
      if ((t != null) && t.isAlive()) {
        myLogger.log("Waiting for Swabra to cleanup previous build files", true);
        try {
          t.join();
        } catch (InterruptedException e) {
          myLogger.log("Swabra: Interrupted while waiting for previous build files cleanup", true);
          myLogger.exception(e);
        }
      }
      myMode = info.getMode();
    }
    try {
      if (!isEnabled(mode)) {
        myLogger.debug("Swabra is disabled", false);
        return;
      }
      mySnapshot = new Snapshot(runningBuild.getAgentTempDirectory(), checkoutDir);
      logSettings(mode, checkoutDir.getAbsolutePath(), myVerbose);
      if (runningBuild.isCleanBuild()) {
        return;
      }
      if (needFullCleanup(myMode)) {
        myDirectoryCleaner.cleanFolder(checkoutDir, new SmartDirectoryCleanerCallback() {
          public void logCleanStarted(File dir) {
            myLogger.log("Swabra: Need a valid checkout directory snapshot - forcing clean checkout for " +
              dir, true);
          }

          public void logFailedToDeleteEmptyDirectory(File dir) {
            myLogger.debug("Swabra: Failed to delete empty checkout directory " + dir.getAbsolutePath(), true);
          }

          public void logFailedToCleanFilesUnderDirectory(File dir) {
            myLogger.debug("Swabra: Failed to delete files in directory " + dir.getAbsolutePath(), true);

          }

          public void logFailedToCleanFile(File file) {
            myLogger.debug("Swabra: Failed to delete file " + file.getAbsolutePath(), true);
          }

          public void logFailedToCleanEntireFolder(File dir) {
            myLogger.debug("Swabra: Failed to delete directory " + dir.getAbsolutePath(), true);
          }
        });
        return;
      }
      if (BEFORE_BUILD.equals(mode)) {
        if (AFTER_BUILD.equals(myMode)) {
          myLogger.debug("Swabra: Will not perform files cleanup, as it occured on previous build finish", false);
        } else {
          myLogger.debug("Swabra: Previous build files cleanup is performed before build", false);
          if (!mySnapshot.collect(myLogger, myVerbose)) {
            logFailedCollect(checkoutDir);
            mode = null;
          }
        }
      } else if (AFTER_BUILD.equals(mode) && BEFORE_BUILD.equals(myMode)) {
        // mode setting changed from "before build" to "after build"
        myLogger.debug("Swabra: Swabra mode setting changed from \"before build\" to \"after build\", " +
          "need to perform build files clean up once before build", false);
        if (!mySnapshot.collect(myLogger, false)) {
          logFailedCollect(checkoutDir);
          mode = null;
        }
      }
    } finally {
      myMode = mode;
      myPrevModes.put(checkoutDir, new PreviousCleanupInfo(myMode));
    }
  }

  private synchronized void logFailedCollect(File checkoutDir) {
    myLogger.warn("Swabra: Couldn't collect files for " + checkoutDir +
      " - will terminate till the end of the build and force clean checkout at next build start", true);
  }

  public void beforeRunnerStart(@NotNull final AgentRunningBuild runningBuild) {
    if (!isEnabled(myMode)) return;
    mySnapshot.snapshot(myLogger, myVerbose);
  }

  public void beforeBuildFinish(@NotNull final BuildFinishedStatus buildStatus) {
    if (AFTER_BUILD.equals(myMode)) {
      myLogger.debug("Swabra: Build files cleanup will be performed after build", true);
    }
  }
  
  public void buildFinished(@NotNull final BuildFinishedStatus buildStatus) {
    if (AFTER_BUILD.equals(myMode)) {
      myLogger.debug("Swabra: Build files cleanup is performed after build", false);
      final File checkoutDir = mySnapshot.getCheckoutDir();
      final Thread t = new Thread(new Runnable() {
        public void run() {
          if (!mySnapshot.collect(myLogger, myVerbose)) {
            logFailedCollect(checkoutDir);
            myPrevModes.put(checkoutDir, null);
          }
        }
      });
      myPrevModes.get(checkoutDir).setWorkingThread(t);
      t.start();
    }
  }

  private void logSettings(String mode, String checkoutDir, boolean verbose) {
    myLogger.debug("Swabra settings: mode = '" + mode +
      "', checkoutDir = " + checkoutDir +
      "', verbose = '" + verbose + "'.", false);
  }

  private static class PreviousCleanupInfo {
    private final String myMode;
    private Thread myWorkingThread;

    public PreviousCleanupInfo(String mode) {
      myMode = mode;
    }

    public void setWorkingThread(@NotNull Thread workingThread) {
      myWorkingThread = workingThread;
    }

    public String getMode() {
      return myMode;
    }

    public Thread getWorkingThread() {
      return myWorkingThread;
    }
  }
}
