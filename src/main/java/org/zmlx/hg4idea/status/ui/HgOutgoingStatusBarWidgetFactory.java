package org.zmlx.hg4idea.status.ui;

import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl(id = "OutHgIncomingOutgoingWidget", order = "after InHgIncomingOutgoingWidget")
public class HgOutgoingStatusBarWidgetFactory extends HgIncomingOutgoingWidgetFactory {
  public HgOutgoingStatusBarWidgetFactory() {
    super(false);
  }
}
