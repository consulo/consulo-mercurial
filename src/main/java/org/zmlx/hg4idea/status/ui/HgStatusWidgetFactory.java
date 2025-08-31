package org.zmlx.hg4idea.status.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.repo.HgRepositoryManager;

import java.util.Objects;

@ExtensionImpl(id = "hgWidget", order = "after codeStyleWidget, before readOnlyWidget")
public class HgStatusWidgetFactory implements StatusBarWidgetFactory {
  @Override
  @Nonnull
  public String getDisplayName() {
    return HgVcsMessages.message("hg4idea.status.bar.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return !HgRepositoryManager.getInstance(project).getRepositories().isEmpty();
  }

  @Override
  @Nonnull
  public StatusBarWidget createWidget(@Nonnull Project project) {
    return new HgStatusWidget(Objects.requireNonNull(HgVcs.getInstance(project)), project, this, HgProjectSettings.getInstance(project));
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return true;
  }
}
