package org.zmlx.hg4idea.ui;

import consulo.configurable.ConfigurationException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.util.HgUtil;

import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Path;
import java.util.List;

/**
 * HgSetExecutablePathPanel is a {@link TextFieldWithBrowseButton}, which opens a file chooser for hg executable
 * and checks validity of the selected file to be an hg executable.
 */
class HgSetExecutablePathPanel extends TextFieldWithBrowseButton {

  private final Set<ActionListener> myOkListeners = new HashSet<>();

  HgSetExecutablePathPanel(final HgProjectSettings projectSettings) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public void validateSelectedPaths(List<Path> paths) throws Exception {
        String path = paths.get(0).toString();
        if (!HgUtil.isExecutableValid(path)) {
          throw new ConfigurationException(HgVcsMessages.message("hg4idea.configuration.executable.error", path));
        }
        for (ActionListener okListener : myOkListeners) {
          okListener.actionPerformed(null);
        }
      }
    };
    addBrowseFolderListener(HgVcsMessages.message("hg4idea.configuration.title"), HgVcsMessages.message("hg4idea.configuration.description"), null, descriptor);
  }

  /**
   * Adds a listener which will be called when file chooser dialog is closed successfully.
   */
  void addOKListener(ActionListener listener) {
    myOkListeners.add(listener);
  }

}
