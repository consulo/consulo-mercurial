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
package org.zmlx.hg4idea.action;

import consulo.mercurial.localize.HgLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgInitCommand;
import org.zmlx.hg4idea.ui.HgInitAlreadyUnderHgDialog;
import org.zmlx.hg4idea.ui.HgInitDialog;
import org.zmlx.hg4idea.util.HgErrorUtil;
import org.zmlx.hg4idea.util.HgUtil;

/**
 * Action for initializing a Mercurial repository.
 * Command "hg init".
 *
 * @author Kirill Likhodedov
 */
public class HgInit extends DumbAwareAction {
    private Project myProject;

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        myProject = e.getData(Project.KEY);
        if (myProject == null) {
            myProject = ProjectManager.getInstance().getDefaultProject();
        }

        // provide window to select the root directory
        final HgInitDialog hgInitDialog = new HgInitDialog(myProject);
        if (!hgInitDialog.showAndGet()) {
            return;
        }
        final VirtualFile selectedRoot = hgInitDialog.getSelectedFolder();
        if (selectedRoot == null) {
            return;
        }

        // check if the selected folder is not yet under mercurial and provide some options in that case
        final VirtualFile vcsRoot = HgUtil.getNearestHgRoot(selectedRoot);
        VirtualFile mapRoot = selectedRoot;
        boolean needToCreateRepo = false;
        if (vcsRoot != null) {
            final HgInitAlreadyUnderHgDialog dialog = new HgInitAlreadyUnderHgDialog(
                myProject,
                selectedRoot.getPresentableUrl(),
                vcsRoot.getPresentableUrl()
            );
            if (!dialog.showAndGet()) {
                return;
            }

            if (dialog.getAnswer() == HgInitAlreadyUnderHgDialog.Answer.USE_PARENT_REPO) {
                mapRoot = vcsRoot;
            }
            else if (dialog.getAnswer() == HgInitAlreadyUnderHgDialog.Answer.CREATE_REPO_HERE) {
                needToCreateRepo = true;
            }
        }
        else { // no parent repository => creating the repository here.
            needToCreateRepo = true;
        }

        if (needToCreateRepo) {
            createRepositoryAsynchronously(selectedRoot, mapRoot);
        }
        else {
            updateDirectoryMappings(mapRoot);
        }
    }

    // update vcs directory mappings if new repository was created inside the current project directory
    private void updateDirectoryMappings(VirtualFile mapRoot) {
        if (myProject != null && !myProject.isDefault() && myProject.getBaseDir() != null
            && VirtualFileUtil.isAncestor(myProject.getBaseDir(), mapRoot, false)) {
            mapRoot.refresh(false, false);
            final String path = mapRoot.equals(myProject.getBaseDir()) ? "" : mapRoot.getPath();
            ProjectLevelVcsManager manager = ProjectLevelVcsManager.getInstance(myProject);
            manager.setDirectoryMappings(VcsUtil.addMapping(manager.getDirectoryMappings(), path, HgVcs.VCS_NAME));
        }
    }

    private void createRepositoryAsynchronously(final VirtualFile selectedRoot, final VirtualFile mapRoot) {
        new HgInitCommand(myProject).executeAsynchronously(
            selectedRoot,
            result -> {
                if (!HgErrorUtil.hasErrorsInCommandExecution(result)) {
                    updateDirectoryMappings(mapRoot);
                    VcsNotifier.getInstance(myProject).notifySuccess(
                        HgLocalize.hg4ideaInitCreatedNotificationTitle().get(),
                        HgLocalize.hg4ideaInitCreatedNotificationDescription(selectedRoot.getPresentableUrl()).get()
                    );
                }
                else {
                    new HgCommandResultNotifier(myProject.isDefault() ? null : myProject).notifyError(
                        result,
                        HgLocalize.hg4ideaInitErrorTitle().get(),
                        HgLocalize.hg4ideaInitErrorDescription(selectedRoot.getPresentableUrl()).get()
                    );
                }
            }
        );
    }
}