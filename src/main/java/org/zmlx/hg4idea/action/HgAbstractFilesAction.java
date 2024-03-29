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
package org.zmlx.hg4idea.action;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.TransactionRunnable;
import consulo.versionControlSystem.VcsException;
import consulo.virtualFileSystem.VirtualFile;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.util.HgUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

abstract class HgAbstractFilesAction extends AnAction {

  private static final Logger LOG = Logger.getInstance(HgAbstractGlobalAction.class.getName());

  protected abstract boolean isEnabled(Project project, HgVcs vcs, VirtualFile file);

  protected abstract void batchPerform(Project project, final HgVcs activeVcs,
                                       List<VirtualFile> files, DataContext context) throws VcsException;

  public final void actionPerformed(AnActionEvent event) {
    final DataContext dataContext = event.getDataContext();

    final Project project = event.getData(Project.KEY);
    final VirtualFile[] files = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (project == null || files == null || files.length == 0) {
      return;
    }

    project.save();

    final HgVcs vcs = HgVcs.getInstance(project);
    if ((vcs == null) || !ProjectLevelVcsManager.getInstance(project).checkAllFilesAreUnder(vcs, files)) {
      return;
    }

    final AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
    List<VcsException> exceptions = helper.runTransactionRunnable(vcs, new TransactionRunnable() {
      public void run(List<VcsException> exceptions) {
        try {
          execute(project, vcs, files, dataContext);
        }
        catch (VcsException ex) {
          exceptions.add(ex);
        }
      }
    }, null);

    helper.showErrors(exceptions, vcs.getName());
  }

  public final void update(AnActionEvent e) {
    super.update(e);

    Presentation presentation = e.getPresentation();
    final DataContext dataContext = e.getDataContext();

    Project project = e.getData(Project.KEY);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    VirtualFile[] files = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (files == null || files.length == 0) {
      presentation.setEnabled(false);
      return;
    }

    final HgVcs vcs = HgVcs.getInstance(project);
    if ((vcs == null)) {
      presentation.setEnabled(false);
      return;
    }

    if (!ProjectLevelVcsManager.getInstance(project).checkAllFilesAreUnder(vcs, files)) {
      presentation.setEnabled(false);
      return;
    }

    boolean enabled = false;
    for (VirtualFile file : files) {
      boolean fileEnabled = isEnabled(project, vcs, file);
      if (fileEnabled) {
        enabled = true;
        break;
      }
    }

    presentation.setEnabled(enabled);
  }

  private void execute(Project project, final HgVcs activeVcs,
                       final VirtualFile[] files, DataContext context) throws VcsException {

    List<VirtualFile> enabledFiles = new LinkedList<>();
    for (VirtualFile file : files) {
      if (isEnabled(project, activeVcs, file)) {
        enabledFiles.add(file);
      }
    }

    batchPerform(project, activeVcs, enabledFiles, context);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        for (VirtualFile file : files) {
          file.refresh(false, true);
        }
      }
    });

    try {
      for (VirtualFile file : enabledFiles) {
        HgUtil.markFileDirty(project, file);
      }
    }
    catch (InvocationTargetException | InterruptedException e) {
      LOG.info("Exception while marking files dirty", e);

    }
  }

}
