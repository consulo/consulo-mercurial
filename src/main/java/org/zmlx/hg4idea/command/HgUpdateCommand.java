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
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgPromptCommandExecutor;
import org.zmlx.hg4idea.provider.update.HgConflictResolver;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.zmlx.hg4idea.util.HgErrorUtil.hasUncommittedChangesConflict;
import static org.zmlx.hg4idea.util.HgUtil.getRepositoryManager;

public class HgUpdateCommand {

  private final Project project;
  private final VirtualFile repo;

  private String revision;
  private boolean clean;

  public HgUpdateCommand(@Nonnull Project project, @Nonnull VirtualFile repo) {
    this.project = project;
    this.repo = repo;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public void setClean(boolean clean) {
    this.clean = clean;
  }


  @Nullable
  public HgCommandResult execute() {
    List<String> arguments = new LinkedList<>();
    if (clean) {
      arguments.add("--clean");
    }

    if (!StringUtil.isEmptyOrSpaces(revision)) {
      arguments.add("--rev");
      arguments.add(revision);
    }

    final HgPromptCommandExecutor executor = new HgPromptCommandExecutor(project);
    executor.setShowOutput(true);
    HgCommandResult result;
    AccessToken token = DvcsUtil.workingTreeChangeStarted(project);
    try {
      result =
        executor.executeInCurrentThread(repo, "update", arguments);
      if (!clean && hasUncommittedChangesConflict(result)) {
        final String message = "<html>Your uncommitted changes couldn't be merged into the requested changeset.<br>" +
                               "Would you like to perform force update and discard them?";
        if (showDiscardChangesConfirmation(project, message) == Messages.OK) {
          arguments.add("-C");
          result = executor.executeInCurrentThread(repo, "update", arguments);
        }
      }
    }
    finally {
      DvcsUtil.workingTreeChangeFinished(project, token);
    }

    VfsUtil.markDirtyAndRefresh(false, true, false, repo);
    return result;
  }

  public static int showDiscardChangesConfirmation(@Nonnull final Project project, @Nonnull final String confirmationMessage) {
    final AtomicInteger exitCode = new AtomicInteger();
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        exitCode.set(Messages.showOkCancelDialog(project, confirmationMessage, "Uncommitted Changes Problem",
                                                 "&Discard Changes", "&Cancel", Messages.getWarningIcon()));
      }
    });
    return exitCode.get();
  }

  public static void updateTo(@Nonnull final String targetRevision, @Nonnull List<HgRepository> repos, @Nullable final Runnable callInAwtLater) {
    FileDocumentManager.getInstance().saveAllDocuments();
    for (HgRepository repo : repos) {
      final VirtualFile repository = repo.getRoot();
      Project project = repo.getProject();
      updateRepoTo(project, repository, targetRevision, callInAwtLater);
    }
  }

  public static void updateRepoTo(@Nonnull final Project project,
                                  @Nonnull final VirtualFile repository,
                                  @Nonnull final String targetRevision,
                                  @Nullable final Runnable callInAwtLater) {
    updateRepoTo(project, repository, targetRevision, false, callInAwtLater);
  }

  public static void updateRepoTo(@Nonnull final Project project,
                                  @Nonnull final VirtualFile repository,
                                  @Nonnull final String targetRevision,
                                  final boolean clean,
                                  @Nullable final Runnable callInAwtLater) {
    new Task.Backgroundable(project, HgVcsMessages.message("action.hg4idea.updateTo.description", targetRevision)) {
      @Override
      public void onSuccess() {
        if (callInAwtLater != null) {
          callInAwtLater.run();
        }
      }

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        updateRepoToInCurrentThread(project, repository, targetRevision, clean);
      }
    }.queue();
  }

  public static boolean updateRepoToInCurrentThread(@Nonnull final Project project,
                                                    @Nonnull final VirtualFile repository,
                                                    @Nonnull final String targetRevision,
                                                    final boolean clean) {
    final HgUpdateCommand hgUpdateCommand = new HgUpdateCommand(project, repository);
    hgUpdateCommand.setRevision(targetRevision);
    hgUpdateCommand.setClean(clean);
    HgCommandResult result = hgUpdateCommand.execute();
    new HgConflictResolver(project).resolve(repository);
    boolean success = !HgErrorUtil.isCommandExecutionFailed(result);
    boolean hasUnresolvedConflicts = HgConflictResolver.hasConflicts(project, repository);
    if (!success) {
      new HgCommandResultNotifier(project).notifyError(result, "", "Update failed");
    }
    else if (hasUnresolvedConflicts) {
      NotificationService.getInstance()
          .newWarn(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION)
          .title(LocalizeValue.localizeTODO("Unresolved conflicts."))
          .content(LocalizeValue.localizeTODO(HgVcsMessages.message("hg4idea.update.warning.merge.conflicts", repository.getPath())))
          .notify(project);
    }
    getRepositoryManager(project).updateRepository(repository);
    HgErrorUtil.markDirtyAndHandleErrors(project, repository);
    return success;
  }
}
