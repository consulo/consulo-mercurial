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
package org.zmlx.hg4idea.repo;

import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.Hash;
import org.zmlx.hg4idea.HgNameWithHashInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public class HgRepoInfo {
  @Nonnull
  private String myCurrentBranch = HgRepository.DEFAULT_BRANCH;
  @Nullable
  private final String myTipRevision;
  @Nullable
  private final String myCurrentRevision;
  @Nonnull
  private final Repository.State myState;
  @Nullable
  private String myCurrentBookmark = null;
  @Nonnull
  private Map<String, LinkedHashSet<Hash>> myBranches = Collections.emptyMap();
  @Nonnull
  private Set<HgNameWithHashInfo> myBookmarks = Collections.emptySet();
  @Nonnull
  private Set<HgNameWithHashInfo> myTags = Collections.emptySet();
  @Nonnull
  private Set<HgNameWithHashInfo> myLocalTags = Collections.emptySet();
  @Nonnull
  private Set<HgNameWithHashInfo> mySubrepos = Collections.emptySet();
  @Nonnull
  private List<HgNameWithHashInfo> myMQApplied = Collections.emptyList();
  @Nonnull
  private List<String> myMqNames = Collections.emptyList();

  public HgRepoInfo(@Nonnull String currentBranch,
                    @Nullable String currentRevision,
                    @Nullable String currentTipRevision,
                    @Nonnull Repository.State state,
                    @Nonnull Map<String, LinkedHashSet<Hash>> branches,
                    @Nonnull Collection<HgNameWithHashInfo> bookmarks,
                    @Nullable String currentBookmark,
                    @Nonnull Collection<HgNameWithHashInfo> tags,
                    @Nonnull Collection<HgNameWithHashInfo> localTags, @Nonnull Collection<HgNameWithHashInfo> subrepos,
                    @Nonnull List<HgNameWithHashInfo> mqApplied, @Nonnull List<String> mqNames) {
    myCurrentBranch = currentBranch;
    myCurrentRevision = currentRevision;
    myTipRevision = currentTipRevision;
    myState = state;
    myBranches = branches;
    myBookmarks = new LinkedHashSet<>(bookmarks);
    myCurrentBookmark = currentBookmark;
    myTags = new LinkedHashSet<>(tags);
    myLocalTags = new LinkedHashSet<>(localTags);
    mySubrepos = new HashSet<>(subrepos);
    myMQApplied = mqApplied;
    myMqNames = mqNames;
  }

  @Nonnull
  public String getCurrentBranch() {
    return myCurrentBranch;
  }

  @Nonnull
  public Map<String, LinkedHashSet<Hash>> getBranches() {
    return myBranches;
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> getBookmarks() {
    return myBookmarks;
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> getTags() {
    return myTags;
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> getLocalTags() {
    return myLocalTags;
  }

  @Nullable
  public String getTipRevision() {
    return myTipRevision;
  }

  @Nullable
  public String getCurrentRevision() {
    return myCurrentRevision;
  }

  @Nullable
  public String getCurrentBookmark() {
    return myCurrentBookmark;
  }

  @Nonnull
  public Repository.State getState() {
    return myState;
  }

  @Nonnull
  public List<HgNameWithHashInfo> getMQApplied() {
    return myMQApplied;
  }

  public List<String> getMqPatchNames() {
    return myMqNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HgRepoInfo info = (HgRepoInfo)o;

    if (myState != info.myState) return false;
    if (myTipRevision != null ? !myTipRevision.equals(info.myTipRevision) : info.myTipRevision != null) return false;
    if (myCurrentRevision != null ? !myCurrentRevision.equals(info.myCurrentRevision) : info.myCurrentRevision != null) return false;
    if (!myCurrentBranch.equals(info.myCurrentBranch)) return false;
    if (myCurrentBookmark != null ? !myCurrentBookmark.equals(info.myCurrentBookmark) : info.myCurrentBookmark != null) return false;
    if (!myBranches.equals(info.myBranches)) return false;
    if (!myBookmarks.equals(info.myBookmarks)) return false;
    if (!myTags.equals(info.myTags)) return false;
    if (!myLocalTags.equals(info.myLocalTags)) return false;
    if (!mySubrepos.equals(info.mySubrepos)) return false;
    if (!myMQApplied.equals(info.myMQApplied)) return false;
    if (!myMqNames.equals(info.myMqNames)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myCurrentBranch, myCurrentRevision, myTipRevision, myCurrentBookmark, myState, myBranches, myBookmarks, myTags,
                        myLocalTags, mySubrepos, myMQApplied, myMqNames);
  }

  @Override
  @Nonnull
  public String toString() {
    return String.format("HgRepository{myCurrentBranch=%s, myCurrentRevision='%s', myState=%s}",
                         myCurrentBranch, myCurrentRevision, myState);
  }

  public boolean hasSubrepos() {
    return !mySubrepos.isEmpty();
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> getSubrepos() {
    return mySubrepos;
  }
}