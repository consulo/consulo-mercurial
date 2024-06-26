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
package org.zmlx.hg4idea.branch;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ide.impl.idea.dvcs.ui.LightActionGroup;
import consulo.ide.impl.idea.dvcs.ui.NewBranchAction;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.CommitChangeListDialog;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.base.HashImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.command.HgBookmarkCommand;
import org.zmlx.hg4idea.command.HgBranchCreateCommand;
import org.zmlx.hg4idea.execution.HgCommandException;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.provider.commit.HgCloseBranchExecutor;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.repo.HgRepositoryManager;
import org.zmlx.hg4idea.ui.HgBookmarkDialog;
import org.zmlx.hg4idea.util.HgErrorUtil;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.*;

import static org.zmlx.hg4idea.util.HgUtil.getNewBranchNameFromUser;
import static org.zmlx.hg4idea.util.HgUtil.getSortedNamesWithoutHashes;

public class HgBranchPopupActions {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final HgRepository myRepository;

  HgBranchPopupActions(@Nonnull Project project, @Nonnull HgRepository repository) {
    myProject = project;
    myRepository = repository;
  }

  ActionGroup createActions() {
    return createActions(null, "");
  }

  ActionGroup createActions(@Nullable LightActionGroup toInsert, @Nonnull String repoInfo) {
    DefaultActionGroup popupGroup = new DefaultActionGroup();
    popupGroup.addAction(new HgNewBranchAction(myProject, Collections.singletonList(myRepository), myRepository));
    popupGroup.addAction(new HgNewBookmarkAction(Collections.singletonList(myRepository), myRepository));
    popupGroup.addAction(new HgBranchPopupActions.HgCloseBranchAction(Collections.singletonList(myRepository), myRepository));
    popupGroup.addAction(new HgShowUnnamedHeadsForCurrentBranchAction(myRepository));
    if (toInsert != null) {
      popupGroup.addAll(toInsert);
    }

    popupGroup.addSeparator("Bookmarks" + repoInfo);
    List<String> bookmarkNames = getSortedNamesWithoutHashes(myRepository.getBookmarks());
    String currentBookmark = myRepository.getCurrentBookmark();
    for (String bookmark : bookmarkNames) {
      AnAction bookmarkAction = new BookmarkActions(myProject, Collections.singletonList(myRepository), bookmark);
      if (bookmark.equals(currentBookmark)) {
        bookmarkAction.getTemplatePresentation().setIcon(AllIcons.Actions.Checked);
      }
      popupGroup.add(bookmarkAction);
    }

    popupGroup.addSeparator("Branches" + repoInfo);
    List<String> branchNamesList = new ArrayList<>(myRepository.getOpenedBranches());//only opened branches have to be shown
    Collections.sort(branchNamesList);
    for (String branch : branchNamesList) {
      if (!branch.equals(myRepository.getCurrentBranch())) { // don't show current branch in the list
        popupGroup.add(new HgCommonBranchActions(myProject, Collections.singletonList(myRepository), branch));
      }
    }
    return popupGroup;
  }

  public static class HgNewBranchAction extends NewBranchAction<HgRepository> {
    @Nonnull
	final HgRepository myPreselectedRepo;

    public HgNewBranchAction(@Nonnull Project project, @Nonnull List<HgRepository> repositories, @Nonnull HgRepository preselectedRepo) {
      super(project, repositories);
      myPreselectedRepo = preselectedRepo;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      final String name = getNewBranchNameFromUser(myPreselectedRepo, "Create New Branch");
      if (name == null) {
        return;
      }
      new Task.Backgroundable(myProject, "Creating " + StringUtil.pluralize("Branch", myRepositories.size()) + "...") {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          createNewBranchInCurrentThread(name);
        }
      }.queue();
    }

    public void createNewBranchInCurrentThread(@Nonnull final String name) {
      for (final HgRepository repository : myRepositories) {
        try {
          HgCommandResult result = new HgBranchCreateCommand(myProject, repository.getRoot(), name).executeInCurrentThread();
          repository.update();
          if (HgErrorUtil.hasErrorsInCommandExecution(result)) {
            new HgCommandResultNotifier(myProject)
              .notifyError(result, "Creation failed", "Branch creation [" + name + "] failed");
          }
        }
        catch (HgCommandException exception) {
          HgErrorUtil.handleException(myProject, "Can't create new branch: ", exception);
        }
      }
    }
  }

  public static class HgCloseBranchAction extends DumbAwareAction {
    @Nonnull
	private final List<HgRepository> myRepositories;
    @Nonnull
	final HgRepository myPreselectedRepo;

    HgCloseBranchAction(@Nonnull List<HgRepository> repositories, @Nonnull HgRepository preselectedRepo) {
      super("Close " + StringUtil.pluralize("branch", repositories.size()),
            "Close current " + StringUtil.pluralize("branch", repositories.size()), PlatformIconGroup.actionsClose());
      myRepositories = repositories;
      myPreselectedRepo = preselectedRepo;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      final Project project = myPreselectedRepo.getProject();
      ApplicationManager.getApplication().saveAll();
      ChangeListManager.getInstance(project)
                       .invokeAfterUpdate(() -> commitAndCloseBranch(project), InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE, VcsBundle
                             .message("waiting.changelists.update.for.show.commit.dialog.message"),
                                          Application.get().getCurrentModalityState());
    }

    private void commitAndCloseBranch(@Nonnull final Project project) {
      final LocalChangeList activeChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
      HgVcs vcs = HgVcs.getInstance(project);
      assert vcs != null;
      final HgRepositoryManager repositoryManager = HgUtil.getRepositoryManager(project);
      List<Change> changesForRepositories = ContainerUtil.filter(activeChangeList.getChanges(),
                                                                 change -> myRepositories.contains(repositoryManager.getRepositoryForFile(
                                                                   ChangesUtil.getFilePath(change))));
      HgCloseBranchExecutor closeBranchExecutor = vcs.getCloseBranchExecutor();
      closeBranchExecutor.setRepositories(myRepositories);
      CommitChangeListDialog.commitChanges(project, changesForRepositories, activeChangeList,
                                           Collections.singletonList(closeBranchExecutor),
                                           false, vcs, "Close Branch", null, false);
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabledAndVisible(ContainerUtil.and(myRepositories,
                                                                 repository -> repository.getOpenedBranches()
                                                                   .contains(repository.getCurrentBranch())));
    }
  }

  public static class HgNewBookmarkAction extends DumbAwareAction {
    @Nonnull
    protected final List<HgRepository> myRepositories;
    @Nonnull
    final HgRepository myPreselectedRepo;

    HgNewBookmarkAction(@Nonnull List<HgRepository> repositories, @Nonnull HgRepository preselectedRepo) {
      super("New Bookmark", "Create new bookmark", AllIcons.General.Add);
      myRepositories = repositories;
      myPreselectedRepo = preselectedRepo;
    }

    @Override
    public void update(AnActionEvent e) {
      if (DvcsUtil.anyRepositoryIsFresh(myRepositories)) {
        e.getPresentation().setEnabled(false);
        e.getPresentation().setDescription("Bookmark creation is not possible before the first commit.");
      }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

      final HgBookmarkDialog bookmarkDialog = new HgBookmarkDialog(myPreselectedRepo);
      if (bookmarkDialog.showAndGet()) {
        final String name = bookmarkDialog.getName();
        if (!StringUtil.isEmptyOrSpaces(name)) {
          HgBookmarkCommand.createBookmarkAsynchronously(myRepositories, name, bookmarkDialog.isActive());
        }
      }
    }
  }

  public static class HgShowUnnamedHeadsForCurrentBranchAction extends ActionGroup {
    @Nonnull
	final HgRepository myRepository;
    @Nonnull
	final String myCurrentBranchName;
    @Nonnull
	Collection<Hash> myHeads = new HashSet<>();

    public HgShowUnnamedHeadsForCurrentBranchAction(@Nonnull HgRepository repository) {
      setPopup(true);
      myRepository = repository;
      myCurrentBranchName = repository.getCurrentBranch();
      getTemplatePresentation().setText(String.format("Unnamed heads for %s", myCurrentBranchName));
      myHeads = filterUnnamedHeads();
    }

    @Nonnull
    private Collection<Hash> filterUnnamedHeads() {
      Collection<Hash> branchWithHashes = myRepository.getBranches().get(myCurrentBranchName);
      String currentHead = myRepository.getCurrentRevision();
      if (branchWithHashes == null || currentHead == null || myRepository.getState() != Repository.State.NORMAL) {
        // repository is fresh or branch is fresh or complex state
        return Collections.emptySet();
      }
      else {
        Collection<Hash> bookmarkHashes = ContainerUtil.map(myRepository.getBookmarks(), info -> info.getHash());
        branchWithHashes.removeAll(bookmarkHashes);
        branchWithHashes.remove(HashImpl.build(currentHead));
      }
      return branchWithHashes;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      List<AnAction> branchHeadActions = new ArrayList<>();
      for (Hash hash : myHeads) {
        branchHeadActions
          .add(new HgCommonBranchActions(myRepository.getProject(), Collections.singletonList(myRepository), hash.toShortString()));
      }
      return branchHeadActions.toArray(new AnAction[branchHeadActions.size()]);
    }

    @Override
    public void update(final AnActionEvent e) {
      if (myRepository.isFresh() || myHeads.isEmpty()) {
        e.getPresentation().setEnabledAndVisible(false);
      }
      else if (!Repository.State.NORMAL.equals(myRepository.getState())) {
        e.getPresentation().setEnabled(false);
      }
    }
  }

  /**
   * Actions available for  bookmarks.
   */
  static class BookmarkActions extends HgCommonBranchActions {

    BookmarkActions(@Nonnull Project project, @Nonnull List<HgRepository> repositories, @Nonnull String branchName) {
      super(project, repositories, branchName);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return ArrayUtil.append(super.getChildren(e), new DeleteBookmarkAction(myProject, myRepositories, myBranchName));
    }

    private static class DeleteBookmarkAction extends HgBranchAbstractAction {

      DeleteBookmarkAction(@Nonnull Project project, @Nonnull List<HgRepository> repositories, @Nonnull String branchName) {
        super(project, "Delete", repositories, branchName);
      }

      @Override
      public void actionPerformed(AnActionEvent e) {
        new Task.Backgroundable(myProject, "Deleting Bookmarks....") {
          @Override
          public void run(@Nonnull ProgressIndicator progressIndicator) {
            for (HgRepository repository : myRepositories) {
              HgBookmarkCommand.deleteBookmarkSynchronously((Project)myProject, repository.getRoot(), myBranchName);
            }
          }
        }.queue();
      }
    }
  }
}
