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
package org.zmlx.hg4idea;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.distributed.branch.DvcsTaskHandler;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.zmlx.hg4idea.branch.HgBranchUtil;
import org.zmlx.hg4idea.command.HgBookmarkCommand;
import org.zmlx.hg4idea.command.HgCommitCommand;
import org.zmlx.hg4idea.command.HgMergeCommand;
import org.zmlx.hg4idea.command.HgUpdateCommand;
import org.zmlx.hg4idea.execution.HgCommandException;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.repo.HgRepositoryManager;
import org.zmlx.hg4idea.util.HgErrorUtil;
import org.zmlx.hg4idea.util.HgReferenceValidator;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class HgTaskHandler extends DvcsTaskHandler<HgRepository> {

  private HgReferenceValidator myNameValidator;

  @Inject
  public HgTaskHandler(@Nonnull Project project) {
    super(HgRepositoryManager.getInstance(project), project, "bookmark");
    myNameValidator = HgReferenceValidator.getInstance();
  }

  @Override
  protected void checkout(@Nonnull String taskName, @Nonnull List<HgRepository> repos, @Nullable Runnable callInAwtLater) {
    HgUpdateCommand.updateTo(
      !HgBranchUtil.getCommonBookmarks(repos).contains(taskName) ? "head() and not bookmark() and branch(\"" + taskName + "\")" : taskName,
      repos, callInAwtLater);
  }

  @Override
  protected void checkoutAsNewBranch(@Nonnull String name, @Nonnull List<HgRepository> repositories) {
    HgBookmarkCommand.createBookmarkAsynchronously(repositories, name, true);
  }

  @Override
  protected String getActiveBranch(HgRepository repository) {
    String bookmark = repository.getCurrentBookmark();
    return bookmark == null ? repository.getCurrentBranch() : bookmark;
  }

  @Nonnull
  @Override
  protected Iterable<TaskInfo> getAllBranches(@Nonnull HgRepository repository) {
    //be careful with equality names of branches/bookmarks =(
    Iterable<String> names =
      ContainerUtil.concat(HgUtil.getSortedNamesWithoutHashes(repository.getBookmarks()), repository.getOpenedBranches());
    return ContainerUtil.map(names, s -> new TaskInfo(s, Collections.singleton(repository.getPresentableUrl())));
  }

  @Override
  protected void mergeAndClose(@Nonnull final String branch, @Nonnull final List<HgRepository> repositories) {
    String bookmarkRevisionArg = "bookmark(\"" + branch + "\")";
    FileDocumentManager.getInstance().saveAllDocuments();
    final UpdatedFiles updatedFiles = UpdatedFiles.create();
    for (final HgRepository repository : repositories) {
      HgMergeCommand.mergeWith(repository, bookmarkRevisionArg, updatedFiles, new Runnable() {

        @Override
        public void run() {
          Project project = repository.getProject();
          VirtualFile repositoryRoot = repository.getRoot();
          try {
            new HgCommitCommand(project, repository, "Automated merge with " + branch).executeInCurrentThread();
            HgBookmarkCommand.deleteBookmarkSynchronously(project, repositoryRoot, branch);
          }
          catch (HgCommandException e) {
            HgErrorUtil.handleException(project, e);
          }
          catch (VcsException e) {
            VcsNotifier.getInstance(project)
                       .notifyError("Exception during merge commit with " + branch, e.getMessage());
          }
        }
      });
    }
  }

  @Override
  protected boolean hasBranch(@Nonnull HgRepository repository, @Nonnull TaskInfo name) {
    return HgUtil.getNamesWithoutHashes(repository.getBookmarks()).contains(name.getName()) || repository.getOpenedBranches()
                                                                                                         .contains(name.getName());
  }

  @Override
  public boolean isBranchNameValid(@Nonnull String branchName) {
    return myNameValidator.checkInput(branchName);
  }

  @Nonnull
  @Override
  public String cleanUpBranchName(@Nonnull String suggestedName) {
    return myNameValidator.cleanUpBranchName(suggestedName);
  }
}
