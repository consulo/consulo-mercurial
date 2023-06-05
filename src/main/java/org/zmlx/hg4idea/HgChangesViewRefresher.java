package org.zmlx.hg4idea;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.change.ChangesViewRefresher;
import org.zmlx.hg4idea.util.HgUtil;

@ExtensionImpl
public class HgChangesViewRefresher implements ChangesViewRefresher {
  @Override
  public void refresh(Project project) {
    HgUtil.getRepositoryManager(project).getRepositories().forEach(r -> r.getLocalIgnoredHolder().startRescan());
  }
}
