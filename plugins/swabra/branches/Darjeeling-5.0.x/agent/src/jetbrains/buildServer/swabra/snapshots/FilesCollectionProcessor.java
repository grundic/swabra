package jetbrains.buildServer.swabra.snapshots;

import jetbrains.buildServer.swabra.SwabraLogger;
import jetbrains.buildServer.swabra.processes.LockedFileResolver;
import jetbrains.buildServer.swabra.snapshots.iteration.FileInfo;
import jetbrains.buildServer.swabra.snapshots.iteration.FilesTraversal;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vbedrosova
 * Date: 04.02.2010
 * Time: 14:35:09
 */
public class FilesCollectionProcessor implements FilesTraversal.ComparisonProcessor {
  @NotNull
  private final SwabraLogger myLogger;
  private final LockedFileResolver myLockedFileResolver;

  private final boolean myStrictDeletion;
  private final boolean myVerbose;

  private int myDetectedNewAndDeleted;
  private int myDetectedModified;
  private int myDetectedDeleted;

  private File myCurrentNewDir;
  private final List<File> myUnableToDeleteFiles;

  private Results myResults;

  public FilesCollectionProcessor(@NotNull SwabraLogger logger,
                                  LockedFileResolver resolver,
                                  boolean verbose,
                                  boolean strict) {
    myLogger = logger;
    myLockedFileResolver = resolver;
    myVerbose = verbose;
    myStrictDeletion = strict;

    myUnableToDeleteFiles = new ArrayList<File>();
  }

  public void processModified(FileInfo info1, FileInfo info2) {
    ++myDetectedModified;
    myLogger.message("Detected modified " + info1.getPath(), myVerbose);
  }

  public void processDeleted(FileInfo info) {
    ++myDetectedDeleted;
    myLogger.message("Detected deleted " + info.getPath(), myVerbose);
  }

  public void processAdded(FileInfo info) {
    final File file = new File(info.getPath());
    if (file.isFile()) {
      deleteObject(file);
    } else {
      if (myCurrentNewDir == null) {
        myCurrentNewDir = file;
      } else if (!file.getAbsolutePath().startsWith(myCurrentNewDir.getAbsolutePath())) {
        deleteSubDirs(myCurrentNewDir);
        myCurrentNewDir = file;
      }
    }
  }

  public void comparisonStarted() {
    myResults = null;
  }

  public void comparisonFinished() {
    if (myCurrentNewDir != null) {
      deleteSubDirs(myCurrentNewDir);
    }

    myResults = new Results(myDetectedNewAndDeleted, myUnableToDeleteFiles.size(),
      myDetectedModified, myDetectedDeleted);

    myDetectedNewAndDeleted = 0;
    myDetectedModified = 0;
    myDetectedDeleted = 0;
    myUnableToDeleteFiles.clear();
    myCurrentNewDir = null;
  }

  public Results getResults() {
    return myResults;
  }

  private void deleteSubDirs(File dir) {
    final File[] files = dir.listFiles();
    if (files != null && files.length > 0) {
      for (File f : files) {
        if (f.isDirectory()) {
          deleteSubDirs(f);
        }
      }
    }
    deleteObject(dir);
  }

  private void deleteObject(File file) {
    if (!resolveDelete(file)) {
      myUnableToDeleteFiles.add(file);
      myLogger.warn("Detected new, unable to delete " + file.getAbsolutePath());
    } else {
      ++myDetectedNewAndDeleted;
      myLogger.message("Detected new and deleted " + file.getAbsolutePath(), myVerbose);
    }
  }

  private boolean resolveDelete(File f) {
    if (!f.exists()) {
      return true;
    }
    if (f.isDirectory() && unableToDeleteDescendant(f)) {
      return false;
    }
    if (FileUtil.delete(f)) {
      return true;
    }
    if (myLockedFileResolver != null) {
      if (myStrictDeletion) {
        return myLockedFileResolver.resolveDelete(f);
      } else {
        return myLockedFileResolver.resolve(f, false);
      }
    }
    return false;
  }

  private boolean unableToDeleteDescendant(File file) {
    final String path = file.getAbsolutePath();
    for (final File f : myUnableToDeleteFiles) {
      if (f.getAbsolutePath().startsWith(path)) {
        return true;
      }
    }
    return false;
  }

  public static final class Results {
    public final int detectedNewAndDeleted;
    public final int detectedNewAndUnableToDelete;
    public final int detectedModified;
    public final int detectedDeleted;

    public Results(int detectedNewAndDeleted,
                   int detectedNewAndUnableToDelete,
                   int detectedModified,
                   int detectedDeleted) {
      this.detectedNewAndDeleted = detectedNewAndDeleted;
      this.detectedNewAndUnableToDelete = detectedNewAndUnableToDelete;
      this.detectedModified = detectedModified;
      this.detectedDeleted = detectedDeleted;
    }
  }
}