// Copyright 2008-2010 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package org.zmlx.hg4idea;

import com.google.common.base.MoreObjects;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.zmlx.hg4idea.util.HgUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.Objects;

public class HgFile {

  private final VirtualFile vcsRoot;
  private final File file;

  private String relativePath;

  public HgFile(@Nonnull VirtualFile vcsRoot, File file) {
    this.vcsRoot = vcsRoot;
    this.file = file;
  }

  public HgFile(@Nonnull VirtualFile vcsRoot, FilePath filePath) {
    this(vcsRoot, filePath.getIOFile());
  }

  public HgFile(@Nonnull Project project, @Nonnull VirtualFile file) {
    this(HgUtil.getHgRootOrNull(project, file), VcsUtil.getFilePath(file.getPath()));
  }

  @Nonnull
  public VirtualFile getRepo() {
    return vcsRoot;
  }

  public File getFile() {
    return file;
  }

  @Nullable
  public String getRelativePath() {
    if (relativePath == null) {
      //For configuration like "d:/.hg" File.getParent method has minimal prefix length, so vcsRoot will be "d:", getParent will be "d:/".
      relativePath = FileUtil.getRelativePath(VfsUtilCore.virtualToIoFile(vcsRoot), file);
    }
    return relativePath;
  }

  @Nonnull
  public FilePath toFilePath() {
    return VcsUtil.getFilePath(file);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HgFile that = (HgFile)o;

    if (!vcsRoot.equals(that.vcsRoot)) {
      return false;
    }
    if (file != null ? !FileUtil.filesEqual(file, that.file) : that.file != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(vcsRoot, file);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(HgFile.class)
      .add("repo", vcsRoot)
      .add("file", file)
      .add("relativePath", getRelativePath())
      .toString();
  }
}