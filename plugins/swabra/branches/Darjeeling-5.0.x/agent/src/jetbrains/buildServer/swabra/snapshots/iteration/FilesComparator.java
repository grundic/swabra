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

package jetbrains.buildServer.swabra.snapshots.iteration;

import java.io.File;
import java.util.Comparator;

/**
 * User: vbedrosova
 * Date: 02.02.2010
 * Time: 13:22:24
 */
public class FilesComparator implements Comparator<File> {
  public int compare(File o1, File o2) {
    return compare(o1.getAbsolutePath(), o1.isFile(), o2.getAbsolutePath(), o2.isFile());
  }

  public static int compare(FileInfo o1, FileInfo o2) {
    return compare(o1.getPath(), o1.isFile(), o2.getPath(), o2.isFile());
  }

  private static int compare(String path1, boolean isFile1, String path2, boolean isFile2) {
    if (isFile1) {
      if (!isFile2) {
        return -1;
      }
    } else {
      if (isFile2) {
        return 1;
      }
    }
    return path1.compareTo(path2);
  }
}