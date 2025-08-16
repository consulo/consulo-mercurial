/**
 * @author VISTALL
 * @since 04/06/2023
 */
open module consulo.mercurial {
  requires consulo.ide.api;

  requires com.google.common;

  // TODO remove in future
  requires java.desktop;
  requires forms.rt;
  requires consulo.ide.impl;

}