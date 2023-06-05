/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zmlx.hg4idea.provider.commit;

import jakarta.annotation.Nonnull;

import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.change.CommitSession;
import org.jetbrains.annotations.Nls;

public class HgMQNewExecutor implements CommitExecutor {
  //todo:should be moved to create patch dialog as an EP -> create patch with...  MQ
  @Nonnull
  private final HgCheckinEnvironment myCheckinEnvironment;

  public HgMQNewExecutor(@Nonnull HgCheckinEnvironment checkinEnvironment) {
    myCheckinEnvironment = checkinEnvironment;
  }

  @Nls
  @Override
  public String getActionText() {
    return "Create M&Q Patch";
  }

  @Nonnull
  @Override
  public CommitSession createCommitSession() {
    myCheckinEnvironment.setMqNew();
    return CommitSession.VCS_COMMIT;
  }
}
