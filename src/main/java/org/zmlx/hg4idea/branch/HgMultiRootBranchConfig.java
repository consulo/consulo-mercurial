/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.zmlx.hg4idea.branch;

import consulo.versionControlSystem.distributed.branch.DvcsMultiRootBranchConfig;
import org.zmlx.hg4idea.repo.HgRepository;

import jakarta.annotation.Nonnull;
import java.util.Collection;

public class HgMultiRootBranchConfig extends DvcsMultiRootBranchConfig<HgRepository> {

  public HgMultiRootBranchConfig(@Nonnull Collection<HgRepository> repositories) {
    super(repositories);
  }

  @Nonnull
  @Override
  public Collection<String> getLocalBranchNames() {
    return HgBranchUtil.getCommonBranches(myRepositories);
  }

  @Nonnull
  Collection<String> getBookmarkNames() {
    return HgBranchUtil.getCommonBookmarks(myRepositories);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (HgRepository repository : myRepositories) {
      sb.append(repository.getPresentableUrl()).append(":").append(repository.getCurrentBranchName()).append(":")
        .append(repository.getState());
    }
    return sb.toString();
  }
}
