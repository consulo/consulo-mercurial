package org.zmlx.hg4idea.command;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsNotifier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.zmlx.hg4idea.util.HgUtil.getRepositoryManager;

public class HgBookmarkCommand {

  public static void createBookmarkAsynchronously(@Nonnull List<HgRepository> repositories, @Nonnull String name, boolean isActive) {
    final Project project = ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(repositories)).getProject();
    if (StringUtil.isEmptyOrSpaces(name)) {
      VcsNotifier.getInstance(project).notifyError("Hg Error", "Bookmark name is empty");
      return;
    }
    new Task.Backgroundable(project, HgVcsMessages.message("hg4idea.progress.bookmark", name)) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        for (HgRepository repository : repositories) {
          executeBookmarkCommandSynchronously(project, repository.getRoot(), name, isActive ? List.of() : singletonList("--inactive"));
        }
      }
    }.queue();
  }

  public static void deleteBookmarkSynchronously(@Nonnull Project project, @Nonnull VirtualFile repo, @Nonnull String name) {
    executeBookmarkCommandSynchronously(project, repo, name, singletonList("-d"));
  }

  private static void executeBookmarkCommandSynchronously(@Nonnull Project project,
                                                          @Nonnull VirtualFile repositoryRoot,
                                                          @Nonnull String name,
                                                          @Nonnull List<String> args) {
    ArrayList<String> arguments = ContainerUtil.newArrayList(args);
    arguments.add(name);
    HgCommandResult result = new HgCommandExecutor(project).executeInCurrentThread(repositoryRoot, "bookmark", arguments);
    getRepositoryManager(project).updateRepository(repositoryRoot);
    if (HgErrorUtil.hasErrorsInCommandExecution(result)) {
      new HgCommandResultNotifier(project)
        .notifyError(result, "Hg Error",
                     String.format("Hg bookmark command failed for repository %s with name %s ", repositoryRoot.getName(), name));
    }
  }
}
