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
package org.zmlx.hg4idea.provider;

import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.CurrentContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.HgRevisionNumber;

class HgCurrentContentRevision extends CurrentContentRevision {
  private final VcsRevisionNumber revisionNumber;

  public HgCurrentContentRevision(HgFile hgFile, VcsRevisionNumber revisionNumber, VirtualFile virtualFile) {
    super(hgFile.toFilePath());
    this.revisionNumber = revisionNumber;
  }

  @Nonnull
  public VcsRevisionNumber getRevisionNumber() {
    return revisionNumber;
  }

  public static ContentRevision create(@Nonnull HgFile hgFile, @Nonnull HgRevisionNumber revision) {
    VirtualFile virtualFile = VcsUtil.getVirtualFile(hgFile.getFile());
    if (virtualFile == null) {
      return null;
    }

    if (!virtualFile.getFileType().isBinary()) {
      return new HgCurrentContentRevision(hgFile, revision, virtualFile);
    }

    return new HgCurrentBinaryContentRevision(hgFile, revision);
  }
}
