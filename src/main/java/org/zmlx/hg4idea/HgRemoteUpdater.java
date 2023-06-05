package org.zmlx.hg4idea;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 05/06/2023
 */
@TopicAPI(ComponentScope.PROJECT)
public interface HgRemoteUpdater extends HgUpdater {
  @Override
  void update(Project project, @Nullable VirtualFile root);
}
