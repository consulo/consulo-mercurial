/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package org.zmlx.hg4idea.repo;

import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import javax.annotation.Nonnull;

import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.provider.HgLocalIgnoredHolder;

import java.util.*;

import javax.annotation.Nullable;


public interface HgRepository extends Repository {
  @Nonnull
  String DEFAULT_BRANCH = "default";

  @Nonnull
  VirtualFile getHgDir();

  /**
   * Returns the current branch of this Hg repository.
   */

  @Nonnull
  String getCurrentBranch();

  /**
   * @return map with heavy branch names and appropriate set of head hashes, order of heads is important - the last head in file is the main
   */
  @Nonnull
  Map<String, LinkedHashSet<Hash>> getBranches();

  /**
   * @return names of opened heavy branches
   */
  @Nonnull
  Set<String> getOpenedBranches();

  @Nonnull
  Collection<HgNameWithHashInfo> getBookmarks();

  @Nonnull
  Collection<HgNameWithHashInfo> getTags();

  @Nonnull
  Collection<HgNameWithHashInfo> getLocalTags();

  @Nullable
  String getCurrentBookmark();

  @Nullable
  String getTipRevision();

  @Nonnull
  HgConfig getRepositoryConfig();

  boolean hasSubrepos();

  @Nonnull
  Collection<HgNameWithHashInfo> getSubrepos();

  @Nonnull
  List<HgNameWithHashInfo> getMQAppliedPatches();

  @Nonnull
  List<String> getAllPatchNames();

  @Nonnull
  List<String> getUnappliedPatchNames();

  void updateConfig();

  HgLocalIgnoredHolder getLocalIgnoredHolder();
}
