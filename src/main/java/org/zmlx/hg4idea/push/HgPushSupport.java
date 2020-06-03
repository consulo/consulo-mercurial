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

import javax.annotation.Nonnull;

import com.intellij.dvcs.branch.DvcsSyncSettings;
import com.intellij.dvcs.push.*;
import com.intellij.dvcs.repo.RepositoryManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.util.ObjectUtils;

import javax.annotation.Nullable;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgUtil;

public class HgPushSupport extends PushSupport<HgRepository, HgPushSource, HgTarget> {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final HgVcs myVcs;
  @Nonnull
  private final HgProjectSettings mySettings;
  @Nonnull
  private final PushSettings myCommonPushSettings;

  public HgPushSupport(@Nonnull Project project) {
    myProject = project;
    myVcs = ObjectUtils.assertNotNull(HgVcs.getInstance(myProject));
    mySettings = myVcs.getProjectSettings();
    myCommonPushSettings = ServiceManager.getService(project, PushSettings.class);
  }

  @Nonnull
  @Override
  public AbstractVcs getVcs() {
    return myVcs;
  }

  @Nonnull
  @Override
  public Pusher<HgRepository, HgPushSource, HgTarget> getPusher() {
    return new HgPusher();
  }

  @Nonnull
  @Override
  public OutgoingCommitsProvider<HgRepository, HgPushSource, HgTarget> getOutgoingCommitsProvider() {
    return new HgOutgoingCommitsProvider();
  }

  @Nullable
  @Override
  public HgTarget getDefaultTarget(@Nonnull HgRepository repository) {
    String defaultPushPath = repository.getRepositoryConfig().getDefaultPushPath();
    return defaultPushPath == null ? null : new HgTarget(defaultPushPath, repository.getCurrentBranchName());
  }

  @Nonnull
  @Override
  public HgPushSource getSource(@Nonnull HgRepository repository) {
    String localBranch = repository.getCurrentBranchName();
    return new HgPushSource(localBranch);
  }

  @Nonnull
  @Override
  public RepositoryManager<HgRepository> getRepositoryManager() {
    return HgUtil.getRepositoryManager(myProject);
  }

  @Nullable
  public VcsPushOptionsPanel createOptionsPanel() {
    return new HgPushOptionsPanel();
  }

  @Override
  @Nonnull
  public PushTargetPanel<HgTarget> createTargetPanel(@Nonnull HgRepository repository, @Nullable HgTarget defaultTarget) {
    return new HgPushTargetPanel(repository, defaultTarget);
  }

  @Override
  public boolean isForcePushAllowed(@Nonnull HgRepository repo, @Nonnull HgTarget target) {
    return true;
  }

  @Override
  public boolean isForcePushEnabled() {
    return true;
  }

  @Override
  public boolean shouldRequestIncomingChangesForNotCheckedRepositories() {
    // load commit for all repositories if sync
    return mySettings.getSyncSetting() == DvcsSyncSettings.Value.SYNC;
  }

  @Override
  public void saveSilentForcePushTarget(@Nonnull HgTarget target) {
    myCommonPushSettings.addForcePushTarget(target.getPresentation(), target.getBranchName());
  }

  @Override
  public boolean isSilentForcePushAllowed(@Nonnull HgTarget target) {
    return myCommonPushSettings.containsForcePushTarget(target.getPresentation(), target.getBranchName());
  }
}
