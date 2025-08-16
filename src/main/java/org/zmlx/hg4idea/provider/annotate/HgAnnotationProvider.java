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
package org.zmlx.hg4idea.provider.annotate;

import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.annotate.AnnotationProviderEx;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.HgFileRevision;
import org.zmlx.hg4idea.HgRevisionNumber;
import org.zmlx.hg4idea.command.HgAnnotateCommand;
import org.zmlx.hg4idea.command.HgWorkingCopyRevisionsCommand;
import org.zmlx.hg4idea.provider.HgHistoryProvider;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.List;

public class HgAnnotationProvider implements AnnotationProviderEx {

  @Nonnull
  private final Project myProject;

  public HgAnnotationProvider(@Nonnull Project project) {
    myProject = project;
  }

  @Nonnull
  public FileAnnotation annotate(@Nonnull VirtualFile file) throws VcsException {
    return annotate(file, null);
  }

  @Nonnull
  public FileAnnotation annotate(@Nonnull VirtualFile file, VcsFileRevision revision) throws VcsException {
    final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(myProject, VcsUtil.getFilePath(file.getPath()));
    if (vcsRoot == null) {
      throw new VcsException("vcs root is null for " + file);
    }
    HgRevisionNumber revisionNumber = revision != null ? (HgRevisionNumber)revision.getRevisionNumber() : null;
    final HgFile hgFile = new HgFile(vcsRoot, VirtualFileUtil.virtualToIoFile(file));
    HgFile fileToAnnotate = revision instanceof HgFileRevision
                            ? HgUtil.getFileNameInTargetRevision(myProject, revisionNumber, hgFile)
                            : new HgFile(vcsRoot,
                                         HgUtil.getOriginalFileName(hgFile.toFilePath(), ChangeListManager.getInstance(myProject)));
    final List<HgAnnotationLine> annotationResult = (new HgAnnotateCommand(myProject)).execute(fileToAnnotate, revisionNumber);
    //for uncommitted renamed file we should provide local name otherwise --follow will fail
    final List<HgFileRevision> logResult =
      HgHistoryProvider.getHistory(revision == null ? hgFile.toFilePath() : fileToAnnotate.toFilePath(), vcsRoot, myProject, null, -1);
    return new HgAnnotation(myProject, hgFile, annotationResult, logResult,
                            revisionNumber != null ? revisionNumber : new HgWorkingCopyRevisionsCommand(myProject).tip(vcsRoot));
  }

  @Nonnull
  @Override
  public FileAnnotation annotate(@Nonnull FilePath path, @Nonnull VcsRevisionNumber revision) throws VcsException {
    final VirtualFile vcsRoot = VcsUtil.getVcsRootFor(myProject, path);
    if (vcsRoot == null) {
      throw new VcsException("vcs root is null for " + path);
    }
    final HgFile hgFile = new HgFile(vcsRoot, path);
    final List<HgAnnotationLine> annotationResult = (new HgAnnotateCommand(myProject)).execute(hgFile, (HgRevisionNumber)revision);
    final List<HgFileRevision> logResult = HgHistoryProvider
      .getHistory(hgFile.toFilePath(), vcsRoot, myProject, (HgRevisionNumber)revision, -1);
    return new HgAnnotation(myProject, hgFile, annotationResult, logResult, revision);
  }
}
