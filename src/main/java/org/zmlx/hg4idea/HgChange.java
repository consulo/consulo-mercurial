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
package org.zmlx.hg4idea;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public final class HgChange {

  @Nonnull
  private HgFile beforeFile;
  @Nonnull
  private HgFile afterFile;
  @Nonnull
  private HgFileStatusEnum status;

  public HgChange(@Nonnull HgFile hgFile, @Nonnull HgFileStatusEnum status) {
    this.beforeFile = hgFile;
    this.afterFile = hgFile;
    this.status = status;
  }

  @Nonnull
  public HgFile beforeFile() {
    return beforeFile;
  }

  @Nonnull
  public HgFile afterFile() {
    return afterFile;
  }

  @Nonnull
  public HgFileStatusEnum getStatus() {
    return status;
  }

  public void setBeforeFile(@Nonnull HgFile beforeFile) {
    this.beforeFile = beforeFile;
  }

  public void setAfterFile(@Nonnull HgFile afterFile) {
    this.afterFile = afterFile;
  }

  public void setStatus(@Nonnull HgFileStatusEnum status) {
    this.status = status;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HgChange change = (HgChange)o;

    if (!afterFile.equals(change.afterFile)) {
      return false;
    }
    if (!beforeFile.equals(change.beforeFile)) {
      return false;
    }
    if (status != change.status) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(beforeFile, afterFile, status);
  }

  @Override
  public String toString() {
    return String.format("HgChange#%s %s => %s", status, beforeFile, afterFile);
  }
}