/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.zmlx.hg4idea.ui;

import consulo.application.Application;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.dvcs.DvcsRememberedInputs;
import consulo.ide.impl.idea.dvcs.ui.CloneDvcsDialog;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgRememberedInputs;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgIdentifyCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.util.HgUtil;

/**
 * A dialog for the mercurial clone options
 */
public class HgCloneDialog extends CloneDvcsDialog {

  public HgCloneDialog(@Nonnull Project project) {
    super(project, HgVcs.DISPLAY_NAME, HgUtil.DOT_HG);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "HgCloneDialog";
  }

  @Override
  protected String getHelpId() {
    return "reference.mercurial.clone.mercurial.repository";
  }

  @Nonnull
  @Override
  protected DvcsRememberedInputs getRememberedInputs() {
    return ServiceManager.getService(HgRememberedInputs.class);
  }

  @Nonnull
  @Override
  protected TestResult test(@Nonnull final String url) {
    HgIdentifyCommand identifyCommand = new HgIdentifyCommand(myProject);
    identifyCommand.setSource(url);
    HgCommandResult result = identifyCommand.execute(Application.get().getModalityStateForComponent(getRootPane()));
    return result != null && result.getExitValue() == 0 ? TestResult.SUCCESS : new TestResult(result.getRawError());
  }
}
