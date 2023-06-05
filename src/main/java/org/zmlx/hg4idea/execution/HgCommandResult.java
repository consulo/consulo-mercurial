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

import consulo.process.util.ProcessOutput;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

import java.util.List;

public final class HgCommandResult {

  //should be deleted and use ProcessOutput without wrapper

  public static final HgCommandResult CANCELLED = new HgCommandResult(new ProcessOutput(1));

  @Nonnull
  private final ProcessOutput myProcessOutput;
  @Nonnull
  private final byte[] myByteArrayOutput;

  public HgCommandResult(@Nonnull ProcessOutput processOutput) {
    this(processOutput, ArrayUtil.EMPTY_BYTE_ARRAY);
  }

  public HgCommandResult(@Nonnull ProcessOutput processOutput, @Nonnull byte[] byteArrayOutput) {
    myProcessOutput = processOutput;
    myByteArrayOutput = byteArrayOutput;
  }

  @Nonnull
  public List<String> getOutputLines() {
    return myProcessOutput.getStdoutLines();
  }

  @Nonnull
  public List<String> getErrorLines() {
    return myProcessOutput.getStderrLines();
  }

  @Nonnull
  public String getRawOutput() {
    return myProcessOutput.getStdout();
  }

  @Nonnull
  public String getRawError() {
    return myProcessOutput.getStderr();
  }

  @Nonnull
  public byte[] getBytesOutput() {
    return myByteArrayOutput;
  }

  public int getExitValue() {
    return myProcessOutput.getExitCode();
  }
}