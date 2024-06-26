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

import consulo.project.Project;
import consulo.util.io.CharsetToolkit;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ByteBackedContentRevision;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.Objects;

public class HgContentRevision implements ByteBackedContentRevision
{

  private final Project myProject;
  @Nonnull
  private final HgFile myHgFile;
  @Nonnull
  private final HgRevisionNumber myRevisionNumber;

  private FilePath filePath;

  protected HgContentRevision(Project project, @Nonnull HgFile hgFile, @Nonnull HgRevisionNumber revisionNumber) {
    myProject = project;
    myHgFile = hgFile;
    myRevisionNumber = revisionNumber;
  }

  @Nonnull
  public static HgContentRevision create(Project project, @Nonnull HgFile hgFile, @Nonnull HgRevisionNumber revisionNumber) {
    return !hgFile.toFilePath().getFileType().isBinary()
           ? new HgContentRevision(project, hgFile, revisionNumber)
           : new HgBinaryContentRevision(project, hgFile, revisionNumber);
  }

  @Nullable
  @Override
  public String getContent() throws VcsException
  {
    if (myRevisionNumber.isWorkingVersion()) return VcsUtil.getFileContent(myHgFile.getFile().getPath());
    final HgFile fileToCat = HgUtil.getFileNameInTargetRevision(myProject, myRevisionNumber, myHgFile);
    return CharsetToolkit.bytesToString(HgUtil.loadContent(myProject, myRevisionNumber, fileToCat), getFile().getCharset());
  }

  public byte[] getContentAsBytes() {
    if (myRevisionNumber.isWorkingVersion()) return VcsUtil.getFileByteContent(myHgFile.getFile());
    final HgFile fileToCat = HgUtil.getFileNameInTargetRevision(myProject, myRevisionNumber, myHgFile);
    return HgUtil.loadContent(myProject, myRevisionNumber, fileToCat);
  }

  @Nonnull
  public FilePath getFile() {
    if (filePath == null) {
      filePath = myHgFile.toFilePath();
    }
    return filePath;
  }

  @Nonnull
  public HgRevisionNumber getRevisionNumber() {
    return myRevisionNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HgContentRevision revision = (HgContentRevision)o;

    if (!myHgFile.equals(revision.myHgFile)) {
      return false;
    }
    if (!myRevisionNumber.equals(revision.myRevisionNumber)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myHgFile, myRevisionNumber);
  }
}