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
package org.zmlx.hg4idea.push;

import consulo.ui.ex.awt.JBCheckBox;
import consulo.versionControlSystem.distributed.push.VcsPushOptionsPanel;

import jakarta.annotation.Nullable;

import java.awt.*;

public class HgPushOptionsPanel extends VcsPushOptionsPanel
{

  private final JBCheckBox myPushBookmarkCheckBox;

  public HgPushOptionsPanel() {
    setLayout(new BorderLayout());
    myPushBookmarkCheckBox = new JBCheckBox("Export Active Bookmarks");
    add(myPushBookmarkCheckBox, BorderLayout.WEST);
  }

  @Override
  @Nullable
  public HgVcsPushOptionValue getValue() {
    return myPushBookmarkCheckBox.isSelected() ? HgVcsPushOptionValue.Current : null;
  }
}
