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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.versionControlSystem.change.VcsAnnotationRefresher;
import consulo.versionControlSystem.distributed.branch.DvcsSyncSettings;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@State(
  name = "hg4idea.settings",
  storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class HgProjectSettings implements PersistentStateComponent<HgProjectSettings.State>, DvcsSyncSettings {

  @Nonnull
  private final HgGlobalSettings myAppSettings;
  @Nonnull
  private final Project myProject;

  private State myState = new State();

  @Inject
  public HgProjectSettings(@Nonnull Project project, @Nonnull HgGlobalSettings appSettings) {
    myProject = project;
    myAppSettings = appSettings;
  }

  public static class State {

    public boolean myCheckIncoming = true;
    public boolean myCheckOutgoing = true;
    public Boolean CHECK_INCOMING_OUTGOING = null;
    public boolean myIgnoreWhitespacesInAnnotations = true;
    public String RECENT_HG_ROOT_PATH = null;
    public Value ROOT_SYNC = Value.NOT_DECIDED;
  }

  public State getState() {
    return myState;
  }

  public void loadState(State state) {
    myState = state;
    if (state.CHECK_INCOMING_OUTGOING == null) {
      state.CHECK_INCOMING_OUTGOING = state.myCheckIncoming || state.myCheckOutgoing;
    }
  }

  public static HgProjectSettings getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, HgProjectSettings.class);
  }

  @Nullable
  public String getRecentRootPath() {
    return myState.RECENT_HG_ROOT_PATH;
  }

  public void setRecentRootPath(@Nonnull String recentRootPath) {
    myState.RECENT_HG_ROOT_PATH = recentRootPath;
  }

  public boolean isCheckIncomingOutgoing() {
    if (myState.CHECK_INCOMING_OUTGOING == null) {
      return myState.myCheckIncoming || myState.myCheckOutgoing;
    }
    return myState.CHECK_INCOMING_OUTGOING.booleanValue();
  }

  public boolean isWhitespacesIgnoredInAnnotations() {
    return myState.myIgnoreWhitespacesInAnnotations;
  }

  @Nonnull
  public Value getSyncSetting() {
    return myState.ROOT_SYNC;
  }

  public void setSyncSetting(@Nonnull Value syncSetting) {
    myState.ROOT_SYNC = syncSetting;
  }

  public void setCheckIncomingOutgoing(boolean checkIncomingOutgoing) {
    myState.CHECK_INCOMING_OUTGOING = checkIncomingOutgoing;
  }

  public void setIgnoreWhitespacesInAnnotations(boolean ignoreWhitespacesInAnnotations) {
    if (myState.myIgnoreWhitespacesInAnnotations != ignoreWhitespacesInAnnotations) {
      myState.myIgnoreWhitespacesInAnnotations = ignoreWhitespacesInAnnotations;
      myProject.getMessageBus().syncPublisher(VcsAnnotationRefresher.class).configurationChanged(HgVcs.getKey());
    }
  }

  public String getHgExecutable() {
    return myAppSettings.getHgExecutable();
  }

  public void setHgExecutable(String text) {
    myAppSettings.setHgExecutable(text);
  }

  @Nonnull
  public HgGlobalSettings getGlobalSettings() {
    return myAppSettings;
  }
}
