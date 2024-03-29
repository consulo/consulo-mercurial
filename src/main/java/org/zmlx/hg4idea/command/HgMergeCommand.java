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
package org.zmlx.hg4idea.command;

import consulo.application.AccessToken;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgPromptCommandExecutor;
import org.zmlx.hg4idea.provider.update.HgConflictResolver;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;
import org.zmlx.hg4idea.util.HgUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import static org.zmlx.hg4idea.HgErrorHandler.ensureSuccess;

public class HgMergeCommand {

  private static final Logger LOG = Logger.getInstance(HgMergeCommand.class.getName());

  @Nonnull
  private final Project project;
  @Nonnull
  private final HgRepository repo;
  @Nullable
  private String revision;

  public HgMergeCommand(@Nonnull Project project, @Nonnull HgRepository repo) {
    this.project = project;
    this.repo = repo;
  }

  private void setRevision(@Nonnull String revision) {
    this.revision = revision;
  }

  @Nullable
  private HgCommandResult executeInCurrentThread() {
    HgPromptCommandExecutor commandExecutor = new HgPromptCommandExecutor(project);
    commandExecutor.setShowOutput(true);
    List<String> arguments = new LinkedList<>();
    if (!StringUtil.isEmptyOrSpaces(revision)) {
      arguments.add("--rev");
      arguments.add(revision);
    }
    AccessToken token = DvcsUtil.workingTreeChangeStarted(project);
    try {
      HgCommandResult result = commandExecutor.executeInCurrentThread(repo.getRoot(), "merge", arguments);
      repo.update();
      return result;
    }
    finally {
      DvcsUtil.workingTreeChangeFinished(project, token);
    }
  }

  @Nullable
  public HgCommandResult mergeSynchronously() throws VcsException {
    HgCommandResult commandResult = ensureSuccess(executeInCurrentThread());
    try {
      HgUtil.markDirectoryDirty(project, repo.getRoot());
    }
    catch (InvocationTargetException | InterruptedException e) {
      throwException(e);
    }

    return commandResult;
  }

  public static void mergeWith(@Nonnull final HgRepository repository,
                               @Nonnull final String branchName,
                               @Nonnull final UpdatedFiles updatedFiles) {
    mergeWith(repository, branchName, updatedFiles, null);
  }

  public static void mergeWith(@Nonnull final HgRepository repository,
                               @Nonnull final String branchName,
                               @Nonnull final UpdatedFiles updatedFiles, @Nullable final Runnable onSuccessHandler) {
    final Project project = repository.getProject();
    final VirtualFile repositoryRoot = repository.getRoot();
    final HgMergeCommand hgMergeCommand = new HgMergeCommand(project, repository);
    hgMergeCommand.setRevision(branchName);//there is no difference between branch or revision or bookmark as parameter to merge,
    // we need just a string
    new Task.Backgroundable(project, "Merging Changes...") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          HgCommandResult result = hgMergeCommand.mergeSynchronously();
          if (HgErrorUtil.isAncestorMergeError(result)) {
            //skip and notify
            VcsNotifier.getInstance(project)
                       .notifyMinorWarning("Merging is skipped for " + repositoryRoot.getPresentableName(),
                                           "Merging with a working directory ancestor has no effect");
            return;
          }
          new HgConflictResolver(project, updatedFiles).resolve(repositoryRoot);
          if (!HgConflictResolver.hasConflicts(project, repositoryRoot) && onSuccessHandler != null) {
            onSuccessHandler.run();    // for example commit changes
          }
        }
        catch (VcsException exception) {
          if (exception.isWarning()) {
            VcsNotifier.getInstance(project).notifyWarning("Warning during merge", exception.getMessage());
          }
          else {
            VcsNotifier.getInstance(project).notifyError("Exception during merge", exception.getMessage());
          }
        }
      }
    }.queue();
  }

  private static void throwException(@Nonnull Exception e) throws VcsException {
    String msg = "Exception during marking directory dirty: " + e;
    LOG.info(msg, e);
    throw new VcsException(msg);
  }
}
