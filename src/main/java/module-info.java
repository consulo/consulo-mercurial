/**
 * @author VISTALL
 * @since 04/06/2023
 */
open module consulo.mercurial {
  requires consulo.application.api;
  requires consulo.component.api;
  requires consulo.configurable.api;
  requires consulo.container.api;
  requires consulo.credential.storage.api;
  requires consulo.datacontext.api;
  requires consulo.disposer.api;
  requires consulo.document.api;
  requires consulo.execution.api;
  requires consulo.file.chooser.api;
  requires consulo.file.editor.api;
  requires consulo.ide.api;
  requires consulo.language.api;
  requires consulo.language.editor.api;
  requires consulo.language.editor.ui.api;
  requires consulo.localize.api;
  requires consulo.logging.api;
  requires consulo.platform.api;
  requires consulo.base.icon.library;
  requires consulo.process.api;
  requires consulo.project.api;
  requires consulo.project.ui.api;
  requires consulo.proxy;
  requires consulo.ui.api;
  requires consulo.ui.ex.api;
  requires consulo.ui.ex.awt.api;
  requires consulo.util.collection;
  requires consulo.util.dataholder;
  requires consulo.util.io;
  requires consulo.util.lang;
  requires consulo.version.control.system.api;
  requires consulo.version.control.system.distributed.api;
  requires consulo.version.control.system.log.api;
  requires consulo.virtual.file.system.api;
  requires consulo.virtual.file.status.api;

  requires com.google.common;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;
  requires miglayout;
}
