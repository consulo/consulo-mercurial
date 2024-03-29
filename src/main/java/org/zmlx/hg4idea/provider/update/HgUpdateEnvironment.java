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
package org.zmlx.hg4idea.provider.update;

import consulo.application.progress.ProgressIndicator;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.update.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.zmlx.hg4idea.ui.HgUpdateDialog;

import javax.swing.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class HgUpdateEnvironment implements UpdateEnvironment {

  private final Project project;
  @Nonnull
  private final HgUpdateConfigurationSettings updateConfiguration;

  public HgUpdateEnvironment(Project project) {
    this.project = project;
    updateConfiguration = ServiceManager.getService(project, HgUpdateConfigurationSettings.class);
  }

  public void fillGroups(UpdatedFiles updatedFiles) {
  }

  @Nonnull
  public UpdateSession updateDirectories(@Nonnull FilePath[] contentRoots,
                                         UpdatedFiles updatedFiles, ProgressIndicator indicator,
                                         @Nonnull Ref<SequentialUpdatesContext> context) {

    List<VcsException> exceptions = new LinkedList<>();

    boolean result = true;
    for (FilePath contentRoot : contentRoots) {
      if (indicator != null) {
        indicator.checkCanceled();
        indicator.startNonCancelableSection();
      }
      VirtualFile repository =
        ProjectLevelVcsManager.getInstance(project).getVcsRootFor(contentRoot);
      if (repository == null) {
        continue;
      }
      try {
        HgUpdater updater = new HgRegularUpdater(project, repository, updateConfiguration);
        result &= updater.update(updatedFiles, indicator, exceptions);
      }
      catch (VcsException e) {
        //TODO include module name where exception occurred
        exceptions.add(e);
      }
      if (indicator != null) {
        indicator.finishNonCancelableSection();
      }
    }
    return new UpdateSessionAdapter(exceptions, !result);
  }

  public Configurable createConfigurable(Collection<FilePath> contentRoots) {
    return new UpdateConfigurable(updateConfiguration);
  }

  public boolean validateOptions(Collection<FilePath> roots) {
    return true;
  }

  public static class UpdateConfigurable implements Configurable {
    private final HgUpdateConfigurationSettings updateConfiguration;
    protected HgUpdateDialog updateDialog;

    public UpdateConfigurable(@Nonnull HgUpdateConfigurationSettings updateConfiguration) {
      this.updateConfiguration = updateConfiguration;
    }

    @Nls
    public String getDisplayName() {
      return "Update";
    }

    public String getHelpTopic() {
      return "reference.VersionControl.Mercurial.UpdateProject";
    }

    public JComponent createComponent() {
      updateDialog = new HgUpdateDialog();
      return updateDialog.getContentPanel();
    }

    public boolean isModified() {
      return true;
    }

    public void apply() throws ConfigurationException {
      updateDialog.applyTo(updateConfiguration);
    }

    public void reset() {
      updateDialog.updateFrom(updateConfiguration);
    }

    public void disposeUIResources() {
      updateDialog = null;
    }
  }

}
