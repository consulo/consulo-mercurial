/**
 * @author VISTALL
 * @since 04/06/2023
 */
open module consulo.mercurial {
  requires consulo.version.control.system.api;
  requires consulo.version.control.system.log.api;
  requires consulo.version.control.system.distributed.api;

  requires consulo.language.api;

  requires consulo.execution.api;

  requires consulo.ide.api; // we need that?

  requires com.google.common;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;
  requires miglayout;
}