/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.zmlx.hg4idea.provider.update;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

@State(
  name = "HgUpdateConfigurationSettings",
  storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class HgUpdateConfigurationSettings implements PersistentStateComponent<HgUpdateConfigurationSettings.State> {
  private State myState = new State();

  public static class State {
    public boolean shouldPull = true;
    @Nonnull
    public HgUpdateType updateType = HgUpdateType.ONLY_UPDATE;
    public boolean shouldCommitAfterMerge = false;
  }

  public void setShouldPull(boolean shouldPull) {
    myState.shouldPull = shouldPull;
  }

  public void setUpdateType(@Nonnull HgUpdateType updateType) {
    myState.updateType = updateType;
  }

  public void setShouldCommitAfterMerge(boolean shouldCommitAfterMerge) {
    myState.shouldCommitAfterMerge = shouldCommitAfterMerge;
  }

  public boolean shouldPull() {
    return myState.shouldPull;
  }

  public HgUpdateType getUpdateType() {
    return myState.updateType;
  }

  public boolean shouldCommitAfterMerge() {
    return myState.updateType == HgUpdateType.MERGE && myState.shouldCommitAfterMerge;
  }

  @Nullable
  @Override
  public HgUpdateConfigurationSettings.State getState() {
    return myState;
  }

  @Override
  public void loadState(HgUpdateConfigurationSettings.State state) {
    myState = state;
  }
}