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

import consulo.versionControlSystem.change.CurrentBinaryContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgFile;

final public class HgCurrentBinaryContentRevision extends CurrentBinaryContentRevision {
  @Nonnull
  private VcsRevisionNumber myRevisionNumber;
  @Nonnull
  private VirtualFile myRepositoryRoot;

  HgCurrentBinaryContentRevision(@Nonnull HgFile hgFile, @Nonnull VcsRevisionNumber revisionNumber) {
    super(hgFile.toFilePath());
    myRepositoryRoot = hgFile.getRepo();
    myRevisionNumber = revisionNumber;
  }

  @Nonnull
  @Override
  public VcsRevisionNumber getRevisionNumber() {
    return myRevisionNumber;
  }

  @Nonnull
  public VirtualFile getRepositoryRoot() {
    return myRepositoryRoot;
  }
}
