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
package org.zmlx.hg4idea.push;

import consulo.versionControlSystem.distributed.push.PushTarget;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.util.HgUtil;

public class HgTarget implements PushTarget {
  @Nonnull
  String myTarget;
  @Nonnull
  String myBranchName;

  public HgTarget(@Nonnull String name, @Nonnull String branchName) {
    myTarget = name;
    myBranchName = branchName;
  }

  @Nonnull
  @Override
  public String getPresentation() {
    return HgUtil.removePasswordIfNeeded(myTarget);
  }

  @Override
  public boolean hasSomethingToPush() {
    // push is always allowed except invalid target
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HgTarget)) return false;

    HgTarget hgTarget = (HgTarget)o;

    if (!myBranchName.equals(hgTarget.myBranchName)) return false;
    if (!myTarget.equals(hgTarget.myTarget)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myTarget.hashCode();
    result = 31 * result + myBranchName.hashCode();
    return result;
  }

  @Nonnull
  public String getBranchName() {
    return myBranchName;
  }
}
