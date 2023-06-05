/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.zmlx.hg4idea.action.mq;

import consulo.ide.impl.idea.util.ContentUtilEx;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.action.HgAbstractGlobalSingleRepoAction;
import org.zmlx.hg4idea.action.HgActionUtil;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.ui.HgMqUnAppliedPatchesPanel;

import java.util.Collection;

public class HgShowUnAppliedPatchesAction extends HgAbstractGlobalSingleRepoAction {
  @Override
  protected void execute(@Nonnull Project project, @Nonnull Collection<HgRepository> repositories, @Nullable HgRepository selectedRepo) {
    if (selectedRepo != null) {
      showUnAppliedPatches(project, selectedRepo);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    HgRepository repository = HgActionUtil.getSelectedRepositoryFromEvent(e);
    e.getPresentation().setEnabledAndVisible(repository != null);
  }

  public static void showUnAppliedPatches(@Nonnull Project project, @Nonnull HgRepository selectedRepo) {
    ToolWindow toolWindow = ObjectUtil.assertNotNull(ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS));
    ContentUtilEx
      .addTabbedContent(toolWindow.getContentManager(), new HgMqUnAppliedPatchesPanel(selectedRepo), "MQ", selectedRepo.getRoot().getName(),
                        true);
    toolWindow.activate(null);
  }
}
