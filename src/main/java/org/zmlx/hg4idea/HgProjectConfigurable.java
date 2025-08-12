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
package org.zmlx.hg4idea;

import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.localize.LocalizeValue;
import consulo.mercurial.localize.HgLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.zmlx.hg4idea.status.ui.HgWidgetUpdater;
import org.zmlx.hg4idea.ui.HgConfigurationProjectPanel;

import javax.swing.*;

public class HgProjectConfigurable implements SearchableConfigurable {
  private final HgConfigurationProjectPanel myPanel;
  @Nonnull
  private final Project myProject;

  public HgProjectConfigurable(@Nonnull Project project, HgProjectSettings projectSettings) {
    myProject = project;
    myPanel = new HgConfigurationProjectPanel(projectSettings, myProject);
  }

  @Nls
  public LocalizeValue getDisplayName() {
    return HgLocalize.hg4ideaMercurial();
  }

  public String getHelpTopic() {
    return "project.propVCSSupport.VCSs.Mercurial";
  }

  public JComponent createComponent() {
    return myPanel.getPanel();
  }

  public boolean isModified() {
    return myPanel.isModified();
  }

  public void apply() throws ConfigurationException
  {
    myPanel.saveSettings();
    myProject.getMessageBus().syncPublisher(HgWidgetUpdater.class).updateVisibility();
  }

  public void reset() {
    myPanel.loadSettings();
  }

  public void disposeUIResources() {
  }

  @Nonnull
  public String getId() {
    return "Mercurial.Project";
  }
}
