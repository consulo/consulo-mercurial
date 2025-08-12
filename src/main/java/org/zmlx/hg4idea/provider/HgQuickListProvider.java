/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zmlx.hg4idea.provider;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.distributed.action.DvcsQuickListContentProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgVcs;

import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class HgQuickListProvider extends DvcsQuickListContentProvider {

  private static final Logger LOG = Logger.getInstance(HgQuickListProvider.class);

  @Nonnull
  @Override
  protected String getVcsName() {
    return HgVcs.VCS_ID;
  }

  @Override
  protected void addVcsSpecificActions(@Nonnull ActionManager manager, @Nonnull List<AnAction> actions) {
    add("hg4idea.branches", manager, actions);
    add("hg4idea.pull", manager, actions);
    add("Vcs.Push", manager, actions);
    add("hg4idea.updateTo", manager, actions);
    add("ChangesView.AddUnversioned", manager, actions);
  }

  public List<AnAction> getNotInVcsActions(@Nullable Project project, @Nullable DataContext dataContext) {
    final String actionName = "Hg.Init";
    final AnAction action = ActionManager.getInstance().getAction(actionName);
    if (action == null) {
      LOG.info("Couldn't find action for name " + actionName);
      return null;
    }
    return Collections.singletonList(action);
  }
}
