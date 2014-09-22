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

import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.swabra.snapshots.iteration.FileInfo;
import jetbrains.buildServer.swabra.snapshots.iteration.FileSystemFilesIterator;
import jetbrains.buildServer.swabra.snapshots.iteration.FilesTraversal;
import jetbrains.buildServer.util.FileUtil;
import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static jetbrains.buildServer.swabra.TestUtil.getTestData;
import static jetbrains.buildServer.swabra.TestUtil.readFile;

/**
 * User: vbedrosova
 * Date: 30.01.2010
 * Time: 17:40:03
 */
public class FileSystemFilesTraversalTest extends TestCase {
  private static final String SVN_FILE = ".svn";

  private void runTest(String resultsFileName) throws Exception {
    final FilesTraversal traversal = new FilesTraversal();
    final StringBuffer results = new StringBuffer();

    final File root = new File(new TempFiles().createTempDir(), "root");
    FileUtil.copyDir(getTestData("filesTraverse", null), root);
    deleteSvnFiles(root);

    traversal.traverse(new FileSystemFilesIterator(root),
      new FilesTraversal.SimpleProcessor() {
        public void process(FileInfo file) {
          results.append(file.getPath()).append("\n");
        }
      });

    final File goldFile = getTestData(resultsFileName + ".gold", null);
    final String resultsFile = goldFile.getAbsolutePath().replace(".gold", ".tmp");

    final String actual = results.toString().replace(root.getAbsolutePath(), "##ROOT##").replace(File.separator, "\\").trim();
    final String expected = readFile(goldFile).trim();
    if (!actual.equals(expected)) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(actual);
      resultsWriter.close();

      assertEquals(actual, expected, actual);
    }
  }

  private void deleteSvnFiles(File root) {
    final List<File> subDirectories = FileUtil.getSubDirectories(root);
    for (File f : subDirectories) {
      if (SVN_FILE.equals(f.getName())) {
        FileUtil.delete(f);
      } else {
        deleteSvnFiles(f);
      }
    }
  }

  public void test1() throws Exception {
    runTest("fileSystemFilesTraversal");
  }
}