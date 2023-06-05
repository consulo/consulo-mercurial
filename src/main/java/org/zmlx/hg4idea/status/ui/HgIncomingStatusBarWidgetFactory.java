package org.zmlx.hg4idea.status.ui;

import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl(id = "InHgIncomingOutgoingWidget", order = "after hgWidget")
public class HgIncomingStatusBarWidgetFactory extends HgIncomingOutgoingWidgetFactory {
  public HgIncomingStatusBarWidgetFactory() {
    super(true);
  }
}
