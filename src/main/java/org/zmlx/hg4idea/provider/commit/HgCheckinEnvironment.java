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
package org.zmlx.hg4idea.provider.commit;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ide.impl.idea.dvcs.AmendComponent;
import consulo.ide.impl.idea.dvcs.push.ui.VcsPushDialog;
import consulo.ide.impl.idea.util.FunctionUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.PairConsumer;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.*;
import org.zmlx.hg4idea.action.HgActionUtil;
import org.zmlx.hg4idea.command.*;
import org.zmlx.hg4idea.command.mq.HgQNewCommand;
import org.zmlx.hg4idea.execution.HgCommandException;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.provider.HgCurrentBinaryContentRevision;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.repo.HgRepositoryManager;
import org.zmlx.hg4idea.util.HgUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.function.Function;

import static consulo.util.lang.ObjectUtil.assertNotNull;
import static org.zmlx.hg4idea.util.HgUtil.getRepositoryManager;

public class HgCheckinEnvironment implements CheckinEnvironment {

  private final Project myProject;
  private boolean myNextCommitIsPushed;
  private boolean myNextCommitAmend; // If true, the next commit is amended
  private boolean myShouldCommitSubrepos;
  private boolean myMqNewPatch;
  private boolean myCloseBranch;
  @Nullable
  private Collection<HgRepository> myRepos;

  public HgCheckinEnvironment(Project project) {
    myProject = project;
  }

  public RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel,
                                                             PairConsumer<Object, Object> additionalDataConsumer) {
    reset();
    return new HgCommitAdditionalComponent(myProject, panel);
  }

  private void reset() {
    myNextCommitIsPushed = false;
    myShouldCommitSubrepos = false;
    myCloseBranch = false;
    myMqNewPatch = false;
    myRepos = null;
  }

  public String getDefaultMessageFor(FilePath[] filesToCheckin) {
    return null;
  }

  public String getHelpId() {
    return null;
  }

  public String getCheckinOperationName() {
    return HgVcsMessages.message("hg4idea.commit");
  }

  public List<VcsException> commit(List<Change> changes,
                                   String preparedComment,
                                   @Nonnull Function<Object, Object> parametersHolder,
                                   Set<String> feedback) {
    List<VcsException> exceptions = new LinkedList<>();
    Map<HgRepository, Set<HgFile>> repositoriesMap = getFilesByRepository(changes);
    addRepositoriesWithoutChanges(repositoriesMap);
    for (Map.Entry<HgRepository, Set<HgFile>> entry : repositoriesMap.entrySet()) {

      HgRepository repo = entry.getKey();
      Set<HgFile> selectedFiles = entry.getValue();
      HgCommitTypeCommand command = myMqNewPatch ? new HgQNewCommand(myProject, repo, preparedComment, myNextCommitAmend) :
        new HgCommitCommand(myProject, repo, preparedComment, myNextCommitAmend, myCloseBranch,
                            myShouldCommitSubrepos && !selectedFiles.isEmpty());

      if (isMergeCommit(repo.getRoot())) {
        //partial commits are not allowed during merges
        //verifyResult that all changed files in the repo are selected
        //If so, commit the entire repository
        //If not, abort

        Set<HgFile> changedFilesNotInCommit = getChangedFilesNotInCommit(repo.getRoot(), selectedFiles);
        boolean partial = !changedFilesNotInCommit.isEmpty();


        if (partial) {
          final StringBuilder filesNotIncludedString = new StringBuilder();
          for (HgFile hgFile : changedFilesNotInCommit) {
            filesNotIncludedString.append("<li>");
            filesNotIncludedString.append(hgFile.getRelativePath());
            filesNotIncludedString.append("</li>");
          }
          if (!mayCommitEverything(filesNotIncludedString.toString())) {
            //abort
            return exceptions;
          }
          //firstly selected changes marked dirty in CommitHelper -> postRefresh, so we need to mark others
          VcsDirtyScopeManager dirtyManager = VcsDirtyScopeManager.getInstance(myProject);
          for (HgFile hgFile : changedFilesNotInCommit) {
            dirtyManager.fileDirty(hgFile.toFilePath());
          }
        }
        // else : all was included, or it was OK to commit everything,
        // so no need to set the files on the command, because then mercurial will complain
      }
      else {
        command.setFiles(selectedFiles);
      }
      try {
        command.executeInCurrentThread();
      }
      catch (HgCommandException e) {
        exceptions.add(new VcsException(e));
      }
      catch (VcsException e) {
        exceptions.add(e);
      }
    }

    // push if needed
    if (myNextCommitIsPushed && exceptions.isEmpty()) {
      final List<HgRepository> preselectedRepositories = ContainerUtil.newArrayList(repositoriesMap.keySet());
      Application application = Application.get();
      application.invokeLater(() -> new VcsPushDialog(myProject, preselectedRepositories, HgUtil.getCurrentRepository(myProject)).show(),
                              application.getDefaultModalityState());
    }

    return exceptions;
  }

  private boolean isMergeCommit(VirtualFile repo) {
    return new HgWorkingCopyRevisionsCommand(myProject).parents(repo).size() > 1;
  }

  private Set<HgFile> getChangedFilesNotInCommit(VirtualFile repo, Set<HgFile> selectedFiles) {
    List<HgRevisionNumber> parents = new HgWorkingCopyRevisionsCommand(myProject).parents(repo);

    HgStatusCommand statusCommand =
      new HgStatusCommand.Builder(true).unknown(false).ignored(false).baseRevision(parents.get(0)).build(myProject);
    Set<HgChange> allChangedFilesInRepo = statusCommand.executeInCurrentThread(repo);

    Set<HgFile> filesNotIncluded = new HashSet<>();

    for (HgChange change : allChangedFilesInRepo) {
      HgFile beforeFile = change.beforeFile();
      HgFile afterFile = change.afterFile();
      if (!selectedFiles.contains(beforeFile)) {
        filesNotIncluded.add(beforeFile);
      }
      else if (!selectedFiles.contains(afterFile)) {
        filesNotIncluded.add(afterFile);
      }
    }
    return filesNotIncluded;
  }

  private boolean mayCommitEverything(final String filesNotIncludedString) {
    final int[] choice = new int[1];
    Runnable runnable = new Runnable() {
      public void run() {
        choice[0] = Messages.showOkCancelDialog(
          myProject,
          HgVcsMessages.message("hg4idea.commit.partial.merge.message", filesNotIncludedString),
          HgVcsMessages.message("hg4idea.commit.partial.merge.title"),
          null
        );
      }
    };
    ApplicationManager.getApplication().invokeAndWait(runnable);
    return choice[0] == Messages.OK;
  }

  public List<VcsException> commit(List<Change> changes, String preparedComment) {
    return commit(changes, preparedComment, FunctionUtil.nullConstant(), null);
  }

  public List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files) {
    final List<HgFile> filesWithRoots = new ArrayList<>();
    for (FilePath filePath : files) {
      VirtualFile vcsRoot = VcsUtil.getVcsRootFor(myProject, filePath);
      if (vcsRoot == null) {
        continue;
      }
      filesWithRoots.add(new HgFile(vcsRoot, filePath));
    }
    new Task.Backgroundable(myProject, "Removing Files...") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        new HgRemoveCommand((Project)myProject).executeInCurrentThread(filesWithRoots);
      }
    }.queue();
    return null;
  }

  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    new HgAddCommand(myProject).addWithProgress(files);
    return null;
  }

  public boolean keepChangeListAfterCommit(ChangeList changeList) {
    return false;
  }

  @Override
  public boolean isRefreshAfterCommitNeeded() {
    return false;
  }

  @Nonnull
  private Map<HgRepository, Set<HgFile>> getFilesByRepository(List<Change> changes) {
    Map<HgRepository, Set<HgFile>> result = new HashMap<>();
    for (Change change : changes) {
      ContentRevision afterRevision = change.getAfterRevision();
      ContentRevision beforeRevision = change.getBeforeRevision();

      if (afterRevision != null) {
        addFile(result, afterRevision);
      }
      if (beforeRevision != null) {
        addFile(result, beforeRevision);
      }
    }
    return result;
  }

  private void addFile(Map<HgRepository, Set<HgFile>> result, ContentRevision contentRevision) {
    FilePath filePath = contentRevision.getFile();
    // try to find repository from hgFile from change: to be able commit sub repositories as expected
    HgRepository repo = HgUtil.getRepositoryForFile(myProject, contentRevision instanceof HgCurrentBinaryContentRevision
      ? ((HgCurrentBinaryContentRevision)contentRevision).getRepositoryRoot()
      : ChangesUtil.findValidParentAccurately(filePath));
    if (repo == null) {
      return;
    }

    Set<HgFile> hgFiles = result.get(repo);
    if (hgFiles == null) {
      hgFiles = new HashSet<>();
      result.put(repo, hgFiles);
    }

    hgFiles.add(new HgFile(repo.getRoot(), filePath));
  }

  public void setNextCommitIsPushed() {
    myNextCommitIsPushed = true;
  }

  public void setMqNew() {
    myMqNewPatch = true;
  }

  public void setCloseBranch(boolean closeBranch) {
    myCloseBranch = closeBranch;
  }

  public void setRepos(@Nonnull Collection<HgRepository> repos) {
    myRepos = repos;
  }

  private void addRepositoriesWithoutChanges(@Nonnull Map<HgRepository, Set<HgFile>> repositoryMap) {
    if (myRepos == null) return;
    for (HgRepository repository : myRepos) {
      if (!repositoryMap.keySet().contains(repository)) {
        repositoryMap.put(repository, Collections.<HgFile>emptySet());
      }
    }
  }

  /**
   * Commit options for hg
   */
  public class HgCommitAdditionalComponent implements RefreshableOnComponent {
    @Nonnull
    private final JPanel myPanel;
    @Nonnull
    private final AmendComponent myAmend;
    @Nonnull
    private final JCheckBox myCommitSubrepos;

    HgCommitAdditionalComponent(@Nonnull Project project, @Nonnull CheckinProjectPanel panel) {
      HgVcs vcs = assertNotNull(HgVcs.getInstance(myProject));

      myAmend = new MyAmendComponent(project, getRepositoryManager(project), panel, "Amend Commit (QRefresh)");
      myAmend.getComponent().setEnabled(vcs.getVersion().isAmendSupported());

      myCommitSubrepos = new JCheckBox("Commit subrepositories", false);
      myCommitSubrepos.setToolTipText(XmlStringUtil.wrapInHtml(
        "Commit all subrepos for selected repositories.<br>" +
          " <code>hg ci <i><b>files</b></i> -S <i><b>subrepos</b></i></code>"));
      myCommitSubrepos.setMnemonic('s');
      Collection<HgRepository> repos = HgActionUtil.collectRepositoriesFromFiles(getRepositoryManager(myProject), panel.getRoots());
      myCommitSubrepos.setVisible(ContainerUtil.exists(repos, HgRepository::hasSubrepos));

      myCommitSubrepos.addActionListener(new MySelectionListener(myAmend.getCheckBox()));
      myAmend.getCheckBox().addActionListener(new MySelectionListener(myCommitSubrepos));

      GridBag gb = new GridBag().
                                  setDefaultInsets(JBUI.insets(2)).
                                  setDefaultAnchor(GridBagConstraints.WEST).
                                  setDefaultWeightX(1).
                                  setDefaultFill(GridBagConstraints.HORIZONTAL);
      myPanel = new JPanel(new GridBagLayout());
      myPanel.add(myAmend.getComponent(), gb.nextLine().next());
      myPanel.add(myCommitSubrepos, gb.nextLine().next());
    }

    @Override
    public void refresh() {
      myAmend.refresh();
      restoreState();
    }

    @Override
    public void saveState() {
      myNextCommitAmend = isAmend();
      myShouldCommitSubrepos = myCommitSubrepos.isSelected();
    }

    @Override
    public void restoreState() {
      myNextCommitAmend = false;
      myShouldCommitSubrepos = false;
    }

    @Override
    public JComponent getComponent() {
      return myPanel;
    }

    public boolean isAmend() {
      return myAmend.isAmend();
    }

    private class MyAmendComponent extends AmendComponent {
      public MyAmendComponent(@Nonnull Project project,
                              @Nonnull HgRepositoryManager repoManager,
                              @Nonnull CheckinProjectPanel panel,
                              @Nonnull String title) {
        super(project, repoManager, panel, title);
      }

      @Nonnull
      @Override
      protected Set<VirtualFile> getVcsRoots(@Nonnull Collection<FilePath> filePaths) {
        return HgUtil.hgRoots(myProject, filePaths);
      }

      @Nullable
      @Override
      protected String getLastCommitMessage(@Nonnull VirtualFile repo) throws VcsException {
        HgCommandExecutor commandExecutor = new HgCommandExecutor(myProject);
        List<String> args = new ArrayList<>();
        args.add("-r");
        args.add(".");
        args.add("--template");
        args.add("{desc}");
        HgCommandResult result = commandExecutor.executeInCurrentThread(repo, "log", args);
        return result == null ? "" : result.getRawOutput();
      }
    }

    private class MySelectionListener implements ActionListener {
      private final JCheckBox myUnselectedComponent;

      public MySelectionListener(JCheckBox unselectedComponent) {
        myUnselectedComponent = unselectedComponent;
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        JCheckBox source = (JCheckBox)e.getSource();
        if (source.isSelected()) {
          myUnselectedComponent.setSelected(false);
          myUnselectedComponent.setEnabled(false);
        }
        else {
          myUnselectedComponent.setEnabled(true);
        }
      }
    }
  }
}
