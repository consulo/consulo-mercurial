package org.zmlx.hg4idea.command;

import consulo.project.Project;
import consulo.ui.ModalityState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgRemoteCommandExecutor;

import java.util.LinkedList;
import java.util.List;

public class HgIdentifyCommand {

  private final Project project;
  private String source;

  public HgIdentifyCommand(Project project) {
    this.project = project;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Nullable
  public HgCommandResult execute(@Nonnull ModalityState state) {
    final List<String> arguments = new LinkedList<>();
    arguments.add(source);
    final HgRemoteCommandExecutor executor = new HgRemoteCommandExecutor(project, source, state, false);
    executor.setSilent(true);
    return executor.executeInCurrentThread(null, "identify", arguments);
  }
}
