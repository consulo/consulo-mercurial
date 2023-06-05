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
package org.zmlx.hg4idea.test;

import consulo.project.Project;
import consulo.ui.ex.errorTreeView.HotfixData;
import com.intellij.openapi.vcs.*;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import com.intellij.openapi.vcs.changes.Change;
import consulo.versionControlSystem.change.CommitResultHandler;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.merge.MergeDialogCustomizer;
import consulo.versionControlSystem.merge.MergeProvider;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Substitutes AbstractVcsHelperImpl for tests, where dialogs need to be tested.
 * Currently it's just a stub implementation notifying listeners about action invoked (which would mean than a dialog would have been
 * shown during normal execution).
 * @author Kirill Likhodedov
 */
public class HgMockVcsHelper extends AbstractVcsHelper {

  private Collection<VcsHelperListener> myListeners = new HashSet<>();

  public HgMockVcsHelper(@Nonnull Project project) {
    super(project);
  }

  @Override
  public void showErrors(List<VcsException> abstractVcsExceptions, @Nonnull String tabDisplayName) {
  }

  @Override
  public void showErrors(Map<HotfixData, List<VcsException>> exceptionGroups, @Nonnull String tabDisplayName) {
  }

  @Override
  public List<VcsException> runTransactionRunnable(AbstractVcs vcs, TransactionRunnable runnable, Object vcsParameters) {
    return null;
  }

  @Override
  public void showAnnotation(FileAnnotation annotation, VirtualFile file, AbstractVcs vcs, int line) {
  }

  @Override
  public void showAnnotation(FileAnnotation annotation, VirtualFile file, AbstractVcs vcs) {
  }

  @Override
  public void showChangesListBrowser(CommittedChangeList changelist, @Nls String title) {
  }

  @Override
  public void showChangesBrowser(List<CommittedChangeList> changelists) {
  }

  @Override
  public void showChangesBrowser(List<CommittedChangeList> changelists, @Nls String title) {
  }

  @Override
  public void showChangesBrowser(CommittedChangesProvider provider,
                                 RepositoryLocation location,
                                 @Nls String title,
                                 @Nullable Component parent) {
  }

  @Override
  public void showWhatDiffersBrowser(@Nullable Component parent, Collection<Change> changes, @Nls String title) {
  }

  @Override
  public <T extends CommittedChangeList, U extends ChangeBrowserSettings> T chooseCommittedChangeList(@Nonnull CommittedChangesProvider<T, U> provider,
                                                                                                      RepositoryLocation location) {
    return null;
  }

  @Override
  public void openCommittedChangesTab(AbstractVcs vcs, VirtualFile root, ChangeBrowserSettings settings, int maxCount, String title) {
  }

  @Override
  public void openCommittedChangesTab(CommittedChangesProvider provider,
                                      RepositoryLocation location,
                                      ChangeBrowserSettings settings,
                                      int maxCount,
                                      String title) {
  }

  @Nonnull
  @Override
  public List<VirtualFile> showMergeDialog(List<VirtualFile> files, MergeProvider provider, @Nonnull MergeDialogCustomizer mergeDialogCustomizer) {
    return null;
  }

  @Override
  public void showFileHistory(@Nonnull VcsHistoryProvider historyProvider, @Nonnull FilePath path, @Nonnull AbstractVcs vcs, String repositoryPath) {
  }

  @Override
  public void showFileHistory(@Nonnull VcsHistoryProvider historyProvider,
                              AnnotationProvider annotationProvider,
                              @Nonnull FilePath path,
                              String repositoryPath,
                              @Nonnull AbstractVcs vcs) {
  }

  @Override
  public void showRollbackChangesDialog(List<Change> changes) {
  }

  @Nullable
  @Override
  public Collection<VirtualFile> selectFilesToProcess(List<VirtualFile> files,
                                                      String title,
                                                      @Nullable String prompt,
                                                      String singleFileTitle,
                                                      String singleFilePromptTemplate,
                                                      VcsShowConfirmationOption confirmationOption) {
    notifyListeners();
    return null;
  }
  
  @Nullable
  @Override
  public Collection<FilePath> selectFilePathsToProcess(List<FilePath> files,
                                                       String title,
                                                       @Nullable String prompt,
                                                       String singleFileTitle,
                                                       String singleFilePromptTemplate,
                                                       VcsShowConfirmationOption confirmationOption) {
    notifyListeners();
    return null;
  }

  @Override
  public boolean commitChanges(@Nonnull Collection<Change> changes, @Nonnull LocalChangeList initialChangeList,
							   @Nonnull String commitMessage, @Nullable CommitResultHandler customResultHandler) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void loadAndShowCommittedChangesDetails(@Nonnull Project project,
                                                 @Nonnull VcsRevisionNumber revision,
                                                 @Nonnull VirtualFile file,
                                                 @Nonnull VcsKey key,
                                                 @Nullable RepositoryLocation location,
                                                 boolean local) {

  }

  public void addListener(VcsHelperListener listener) {
    myListeners.add(listener);
  }

  private void notifyListeners() {
    for (VcsHelperListener listener : myListeners) {
      listener.dialogInvoked();
    }
  }

}
