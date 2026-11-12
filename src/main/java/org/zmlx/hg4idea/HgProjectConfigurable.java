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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.Task;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.mercurial.localize.HgLocalize;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.distributed.branch.DvcsSyncSettings;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.zmlx.hg4idea.repo.HgRepositoryManager;
import org.zmlx.hg4idea.util.HgVersion;

/**
 * The Mercurial page. Its check boxes are bound to the settings with method references, so they are switched from Find
 * Action too - keep them so.
 */
@ExtensionImpl
public class HgProjectConfigurable extends SimpleConfigurableByProperties implements ProjectConfigurable {
  private final Project myProject;
  private final HgProjectSettings mySettings;

  @Inject
  public HgProjectConfigurable(Project project, HgProjectSettings settings) {
    myProject = project;
    mySettings = settings;
  }

  @Override
  public String getId() {
    return "vcs.Mercurial";
  }

  @Override
  public @Nullable String getParentId() {
    return StandardConfigurableIds.VCS_GROUP;
  }

  @Override
  public LocalizeValue getDisplayName() {
    return HgLocalize.hg4ideaMercurial();
  }

  @Override
  public @Nullable String getHelpTopic() {
    return "project.propVCSSupport.VCSs.Mercurial";
  }

  @RequiredUIAccess
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
    HgProjectSettings settings = mySettings;
    VerticalLayout root = VerticalLayout.create();

    FileChooserTextBoxBuilder pathBuilder = FileChooserTextBoxBuilder.create(myProject);
    pathBuilder.uiDisposable(uiDisposable);
    pathBuilder.dialogTitle(HgLocalize.hg4ideaConfigurationExecutableTitle());
    pathBuilder.dialogDescription(HgLocalize.hg4ideaConfigurationDescription());
    pathBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    FileChooserTextBoxBuilder.Controller pathField = pathBuilder.build();
    propertyBuilder.add(
      () -> pathField.getValue().trim(),
      pathField::setValue,
      settings::getHgExecutable,
      path -> {
        settings.setHgExecutable(path);
        HgVcs vcs = HgVcs.getInstance(myProject);
        if (vcs != null) {
          vcs.checkVersion();
        }
      }
    );
    Button testButton = Button.create(HgLocalize.hg4ideaConfigurationTest());
    testButton.addClickListener(e -> testExecutable(root, pathField.getValue().trim()));
    root.add(DockLayout.create()
      .left(Label.create(HgLocalize.hg4ideaConfigurationExecutableTitle()))
      .center(pathField)
      .right(testButton));

    CheckBox checkIncomingOutgoing = CheckBox.create(HgLocalize.hg4ideaConfigurationCheckIncomingOutgoing());
    propertyBuilder.add(checkIncomingOutgoing, settings::isCheckIncomingOutgoing, settings::setCheckIncomingOutgoing);
    root.add(checkIncomingOutgoing);

    CheckBox ignoreWhitespaces = CheckBox.create(HgLocalize.hg4ideaConfigurationIgnoreWhitespaceInAnnotate());
    propertyBuilder.add(ignoreWhitespaces, settings::isWhitespacesIgnoredInAnnotations, settings::setIgnoreWhitespacesInAnnotations);
    root.add(ignoreWhitespaces);

    CheckBox syncControl = CheckBox.create(DistributedVcsLocalize.syncSetting());
    syncControl.setToolTipText(DistributedVcsLocalize.syncSettingDescription(HgLocalize.hg4ideaMercurial()));
    if (!myProject.isDefault()) {
      HgRepositoryManager repositoryManager = HgRepositoryManager.getInstance(myProject);
      syncControl.setVisible(repositoryManager != null && repositoryManager.moreThanOneRoot());
    }
    propertyBuilder.add(
      syncControl,
      () -> settings.getSyncSetting() == DvcsSyncSettings.Value.SYNC,
      sync -> settings.setSyncSetting(sync ? DvcsSyncSettings.Value.SYNC : DvcsSyncSettings.Value.DONT_SYNC)
    );
    root.add(syncControl);
    return root;
  }

  @RequiredUIAccess
  private void testExecutable(Component parent, String executable) {
    UIAccess uiAccess = UIAccess.current();
    Task.Backgroundable.queue(myProject, HgLocalize.hg4ideaConfigurationTitle(), true, indicator -> {
      HgVersion version;
      try {
        version = HgVersion.identifyVersion(executable);
      }
      catch (Exception e) {
        uiAccess.give(() -> Alerts.okError(LocalizeValue.of(String.valueOf(e.getMessage())))
          .title(HgLocalize.hg4ideaRunFailedTitle())
          .showAsync(parent));
        return;
      }
      uiAccess.give(() -> Alerts.okInfo(LocalizeValue.localizeTODO("Mercurial version is " + version))
        .title(HgLocalize.hg4ideaRunSuccessTitle())
        .showAsync(parent));
    });
  }
}
