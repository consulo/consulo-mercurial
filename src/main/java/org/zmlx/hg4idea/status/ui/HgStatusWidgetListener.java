package org.zmlx.hg4idea.status.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryMappingListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@TopicImpl(ComponentScope.PROJECT)
public class HgStatusWidgetListener implements VcsRepositoryMappingListener {
  private final Project myProject;

  @Inject
  public HgStatusWidgetListener(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void mappingChanged() {
    UIAccess uiAccess = myProject.getUIAccess();
    StatusBarWidgetsManager.getInstance(myProject).updateWidget(HgStatusWidgetFactory.class, uiAccess);
  }
}
