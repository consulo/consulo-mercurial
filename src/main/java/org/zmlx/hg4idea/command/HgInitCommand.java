package org.zmlx.hg4idea.command;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgCommandResultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the "hg init"
 */
public class HgInitCommand {

  private final Project myProject;

  public HgInitCommand(@Nonnull Project project) {
    myProject = project;
  }

  public void executeAsynchronously(@Nonnull VirtualFile repositoryRoot, final HgCommandResultHandler resultHandler) {
    final List<String> args = new ArrayList<>(1);
    args.add(repositoryRoot.getPath());
    final HgCommandExecutor executor = new HgCommandExecutor(myProject, repositoryRoot.getPath());
    executor.setShowOutput(true);
    executor.execute(null, "init", args, new HgCommandResultHandler() {
      @Override
      public void process(@Nullable HgCommandResult result) {
        resultHandler.process(result);
      }
    });
  }

}
