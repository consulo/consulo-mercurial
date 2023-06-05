package org.zmlx.hg4idea.status.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryMappingListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@TopicImpl(ComponentScope.PROJECT)
public class HgIncomingOutgoingWidgetMappingListener implements VcsRepositoryMappingListener {
  private final Project myProject;

  @Inject
  public HgIncomingOutgoingWidgetMappingListener(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void mappingChanged() {
    updateAll(myProject);
  }

  public static void updateAll(Project project) {
    UIAccess uiAccess = project.getApplication().getLastUIAccess();

    StatusBarWidgetsManager widgetsManager = StatusBarWidgetsManager.getInstance(project);
    widgetsManager.updateWidget(HgIncomingStatusBarWidgetFactory.class, uiAccess);
    widgetsManager.updateWidget(HgOutgoingStatusBarWidgetFactory.class, uiAccess);
  }
}
