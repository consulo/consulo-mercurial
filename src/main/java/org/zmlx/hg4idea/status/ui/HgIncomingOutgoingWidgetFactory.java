package org.zmlx.hg4idea.status.ui;

import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;

import java.util.Objects;

abstract class HgIncomingOutgoingWidgetFactory implements StatusBarWidgetFactory {
  private final boolean myIsIncoming;

  HgIncomingOutgoingWidgetFactory(boolean isIncoming) {
    myIsIncoming = isIncoming;
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return HgProjectSettings.getInstance(project).isCheckIncomingOutgoing();
  }

  @Override
  @Nonnull
  public StatusBarWidget createWidget(@Nonnull Project project) {
    HgVcs hgVcs = Objects.requireNonNull(HgVcs.getInstance(project));
    return new HgIncomingOutgoingWidget(hgVcs, myIsIncoming);
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    consulo.disposer.Disposer.dispose(widget);
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return true;
  }

  @Override
  public
  @Nonnull
  String getId() {
    return myIsIncoming ? HgIncomingOutgoingWidget.INCOMING_WIDGET_ID : HgIncomingOutgoingWidget.OUTGOING_WIDGET_ID;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return myIsIncoming ? HgVcsMessages.message("hg4idea.status.bar.incoming.widget.name")
      : HgVcsMessages.message("hg4idea.status.bar.outgoing.widget.name");
  }
}
