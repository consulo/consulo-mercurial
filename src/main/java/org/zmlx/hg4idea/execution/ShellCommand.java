// Copyright 2008-2010 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package org.zmlx.hg4idea.execution;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.process.*;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.util.CapturingProcessAdapter;
import consulo.process.util.ProcessOutput;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsLocaleHelper;
import consulo.versionControlSystem.util.LineHandlerHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public final class ShellCommand {
  private final GeneralCommandLine myCommandLine;

  public ShellCommand(@Nonnull List<String> commandLine, @Nullable String dir, @Nullable Charset charset) {
    if (commandLine.isEmpty()) {
      throw new IllegalArgumentException("commandLine is empty");
    }
    myCommandLine = new GeneralCommandLine(commandLine);
    if (dir != null) {
      myCommandLine.setWorkingDirectory(Path.of(dir));
    }
    if (charset != null) {
      myCommandLine.setCharset(charset);
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      //ignore all hg config files except current repository config
      myCommandLine.getEnvironment().put("HGRCPATH", "");
    }
    myCommandLine.withEnvironment(VcsLocaleHelper.getDefaultLocaleEnvironmentVars("hg"));
  }

  @Nonnull
  public HgCommandResult execute(final boolean showTextOnIndicator, boolean isBinary) throws ShellCommandException, InterruptedException {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    try {
      ProcessHandler processHandler;
      ProcessHandlerBuilder builder = ProcessHandlerBuilder.create(myCommandLine);
      if (isBinary) {
        builder = builder.binary();
      }
      processHandler = builder.build();
      CapturingProcessAdapter outputAdapter = new CapturingProcessAdapter() {

        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          Iterator<String> lines = LineHandlerHelper.splitText(event.getText()).iterator();
          if (ProcessOutputTypes.STDOUT == outputType) {
            while (lines.hasNext()) {
              String line = lines.next();
              if (indicator != null && showTextOnIndicator) {
                indicator.setText2(line);
              }
              addToOutput(line, ProcessOutputTypes.STDOUT);
            }
          }
          else {
            super.onTextAvailable(event, outputType);
          }
        }
      };
      processHandler.addProcessListener(outputAdapter);
      processHandler.startNotify();
      while (!processHandler.waitFor(300)) {
        if (indicator != null && indicator.isCanceled()) {
          processHandler.destroyProcess();
          outputAdapter.getOutput().setExitCode(255);
          break;
        }
      }
      ProcessOutput output = outputAdapter.getOutput();
      return isBinary ? new HgCommandResult(output, ((BinaryProcessHandler)processHandler).getOutput()) : new HgCommandResult(output);
    }
    catch (ExecutionException e) {
      throw new ShellCommandException(e);
    }
  }
}