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
package org.zmlx.hg4idea.ui;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;

import javax.swing.*;

public class HgGlobalStatusDialog extends DialogWrapper
{
  private JPanel contentPanel;
  private JTextArea outputTextArea;

  public HgGlobalStatusDialog(Project project) {
    super(project, false);
    init();
  }

  public void append(String text) {
    if (StringUtil.isEmpty(text)) {
      return;
    }
    outputTextArea.append(text);
  }

  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }

  protected JComponent createCenterPanel() {
    return contentPanel;
  }

}
