/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.zmlx.hg4idea.provider;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.history.BaseDiffFromHistoryHandler;
import consulo.versionControlSystem.history.DiffFromHistoryHandler;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgFileRevision;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.List;

/**
 * {@link DiffFromHistoryHandler#showDiffForTwo(Project, FilePath, VcsFileRevision, VcsFileRevision) "Show Diff" for 2 revision} calls the common code.
 */
public class HgDiffFromHistoryHandler extends BaseDiffFromHistoryHandler<HgFileRevision> {

  private static final Logger LOG = Logger.getInstance(HgDiffFromHistoryHandler.class);

  public HgDiffFromHistoryHandler(@Nonnull Project project) {
    super(project);
  }

  @Nonnull
  @Override
  protected List<Change> getChangesBetweenRevisions(@Nonnull FilePath path, @Nonnull HgFileRevision rev1, @Nullable HgFileRevision rev2) {
    return executeDiff(path, rev1, rev2);
  }

  @Nonnull
  @Override
  protected List<Change> getAffectedChanges(@Nonnull FilePath path, @Nonnull HgFileRevision rev) throws VcsException {
    return executeDiff(path, null, rev);
  }

  @Nonnull
  @Override
  protected String getPresentableName(@Nonnull HgFileRevision revision) {
    return revision.getRevisionNumber().getChangeset();
  }

  @Nonnull
  private List<Change> executeDiff(@Nonnull FilePath path, @Nullable HgFileRevision rev1, @Nullable HgFileRevision rev2) {
    VirtualFile root = VcsUtil.getVcsRootFor(myProject, path);
    LOG.assertTrue(root != null, "Repository is null for " + path);

    return HgUtil
      .getDiff(myProject, root, path, rev1 != null ? rev1.getRevisionNumber() : null, rev2 != null ? rev2.getRevisionNumber() : null);
  }
}