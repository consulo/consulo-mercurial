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

import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.ui.HgMqUnAppliedPatchesPanel;

import java.util.List;

public abstract class HgSingleActionFomMqPatches extends HgActionFromMqPatches {
  @Override
  protected void executeInCurrentThread(@Nonnull HgRepository repository, @Nonnull List<String> patchNames) {
    assert patchNames.size() == 1;
    executeInCurrentThread(repository, patchNames.iterator().next());
  }

  @Override
  public void update(AnActionEvent e) {
    HgMqUnAppliedPatchesPanel patchInfo = e.getData(HgMqUnAppliedPatchesPanel.MQ_PATCHES);
    e.getPresentation().setEnabled(patchInfo != null && patchInfo.getSelectedRowsCount() == 1);
  }

  protected abstract void executeInCurrentThread(@Nonnull HgRepository repository, @Nonnull String patchName);
}
