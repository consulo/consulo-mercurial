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

import com.google.common.io.BaseEncoding;
import consulo.ide.ServiceManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.repository.RepoStateException;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsLogObjectsFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.util.HgVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads information about the Hg repository from Hg service files located in the {@code .hg} folder.
 * NB: reads the files where they are (the repository may be of a remote machine). Consider using caching.
 * Throws a {@link RepoStateException} in the case of incorrect Hg file format.
 *
 * @author Nadya Zabrodina
 */
public class HgRepositoryReader {

  private static final Logger LOG = Logger.getInstance(HgRepositoryReader.class);

  private static final Pattern HASH_NAME = Pattern.compile("\\s*([0-9a-fA-F]{40})[:?|\\s+](.+)");
  private static final Pattern HASH_STATUS_NAME = Pattern.compile("\\s*([0-9a-fA-F]+)\\s+\\w\\s+(.+)");
  //hash + name_or_revision_num; hash + status_character +  name_or_revision_num

  @Nonnull
  private final Path myHgDir;            // .hg
  @Nonnull
  private Path myBranchHeadsFile;  // .hg/cache/branch* + part depends on version
  @Nonnull
  private final Path myCacheDir; // .hg/cache (does not exist before first commit)
  @Nonnull
  private final Path myCurrentBranch;    // .hg/branch
  @Nonnull
  private final Path myBookmarksFile; //.hg/bookmarks
  @Nonnull
  private final Path myCurrentBookmark; //.hg/bookmarks.current
  @Nonnull
  private final Path myTagsFile; //.hgtags  - not in .hg directory!!!
  @Nonnull
  private final Path myLocalTagsFile;  // .hg/localtags
  @Nonnull
  private final Path myDirStateFile;  // .hg/dirstate
  @Nonnull
  private final Path mySubrepoFile;  // .hgsubstate

  //mq internal files
  @Nonnull
  private final Path myMqInternalDir; //.hg/patches

  @Nonnull
  private final VcsLogObjectsFactory myVcsObjectsFactory;
  private final boolean myStatusInBranchFile;
  @Nonnull
  private final HgVcs myVcs;

  public HgRepositoryReader(@Nonnull HgVcs vcs, @Nonnull Path hgDir) {
    myHgDir = hgDir;
    DvcsUtil.assertFileExists(myHgDir, ".hg directory not found in " + myHgDir);
    myVcs = vcs;
    HgVersion version = myVcs.getVersion();
    myStatusInBranchFile = version.hasBranch2();
    myCacheDir = myHgDir.resolve("cache");
    myBranchHeadsFile = identifyBranchHeadFile(version, myCacheDir);
    myCurrentBranch = myHgDir.resolve("branch");
    myBookmarksFile = myHgDir.resolve("bookmarks");
    myCurrentBookmark = myHgDir.resolve("bookmarks.current");
    myLocalTagsFile = myHgDir.resolve("localtags");
    myTagsFile = myHgDir.resolveSibling(".hgtags");
    mySubrepoFile = myHgDir.resolveSibling(".hgsubstate");
    myDirStateFile = myHgDir.resolve("dirstate");
    myMqInternalDir = myHgDir.resolve("patches");
    myVcsObjectsFactory = ServiceManager.getService(vcs.getProject(), VcsLogObjectsFactory.class);
  }

  /**
   * Identify file with branches and heads information depends on hg version;
   */
  @Nonnull
  private static Path identifyBranchHeadFile(@Nonnull HgVersion version, @Nonnull Path parentCacheFile) {
    //before 2.5 only branchheads exist; branchheads-served after mercurial 2.5; branch2-served after 2.9;
    // when project is  recently  cloned there are only base file
    if (version.hasBranch2()) {
      Path file = parentCacheFile.resolve("branch2-served");
      return Files.exists(file) ? file : parentCacheFile.resolve("branch2-base");
    }
    if (version.hasBranchHeadsBaseServed()) {
      Path file = parentCacheFile.resolve("branchheads-served");
      return Files.exists(file) ? file : parentCacheFile.resolve("branchheads-base");
    }
    return parentCacheFile.resolve("branchheads");
  }

  /**
   * Finds current revision value.
   *
   * @return The current revision hash, or <b>{@code null}</b> if current revision is unknown - it is the initial repository state.
   */
  @Nullable
  public String readCurrentRevision() {
    if (!isDirStateInfoAvailable()) return null;
    try {
      return BaseEncoding.base16().lowerCase().encode(readHashBytesFromFile(myDirStateFile));
    }
    catch (IOException e) {
      // dirState exists if not fresh,  if we could not load dirState info repository must be corrupted
      LOG.error("IOException while trying to read current repository state information.", e);
      return null;
    }
  }

  @Nonnull
  private static byte[] readHashBytesFromFile(@Nonnull Path file) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      return FileUtil.loadBytes(stream, 20);
    }
  }

  /**
   * Finds tip revision value.
   *
   * @return The tip revision hash, or <b>{@code null}</b> if tip revision is unknown - it is the initial repository state.
   */
  @Nullable
  public String readCurrentTipRevision() {
    if (!isBranchInfoAvailable()) return null;
    String[] branchesWithHeads;
    try {
      branchesWithHeads = DvcsUtil.tryLoadFile(myBranchHeadsFile).split("\n");
    }
    catch (RepoStateException e) {
      LOG.error(e);
      return null;
    }
    String head = branchesWithHeads[0];
    Matcher matcher = HASH_NAME.matcher(head);
    if (matcher.matches()) {
      return (matcher.group(1));
    }
    return null;
  }

  private boolean isBranchInfoAvailable() {
    myBranchHeadsFile = identifyBranchHeadFile(myVcs.getVersion(), myCacheDir);
    return !isFresh() && Files.exists(myBranchHeadsFile);
  }

  private boolean isDirStateInfoAvailable() {
    return Files.exists(myDirStateFile);
  }

  /**
   * Return current branch
   */
  @Nonnull
  public String readCurrentBranch() {
    return branchExist() ? DvcsUtil.tryLoadFileOrReturn(myCurrentBranch, HgRepository.DEFAULT_BRANCH) : HgRepository.DEFAULT_BRANCH;
  }

  @Nonnull
  public Map<String, LinkedHashSet<Hash>> readBranches() {
    Map<String, LinkedHashSet<Hash>> branchesWithHashes = new HashMap<>();
    // Set<String> branchNames = new HashSet<String>();
    if (isBranchInfoAvailable()) {
      Pattern activeBranchPattern = myStatusInBranchFile ? HASH_STATUS_NAME : HASH_NAME;
      String[] branchesWithHeads = DvcsUtil.tryLoadFileOrReturn(myBranchHeadsFile, "").split("\n");
      // first one - is a head revision: head hash + head number;
      for (int i = 1; i < branchesWithHeads.length; ++i) {
        Matcher matcher = activeBranchPattern.matcher(branchesWithHeads[i]);
        if (matcher.matches()) {
          String name = matcher.group(2);
          if (branchesWithHashes.containsKey(name)) {
            branchesWithHashes.get(name).add(myVcsObjectsFactory.createHash(matcher.group(1)));
          }
          else {
            LinkedHashSet<Hash> hashes = new LinkedHashSet<>();
            hashes.add(myVcsObjectsFactory.createHash(matcher.group(1)));
            branchesWithHashes.put(name, hashes);
          }
        }
      }
    }
    return branchesWithHashes;
  }

  private boolean isMergeInProgress() {
    return Files.exists(myHgDir.resolve("merge"));
  }

  private boolean hasSubrepos() {
    return Files.exists(mySubrepoFile);
  }

  private boolean isRebaseInProgress() {
    return Files.exists(myHgDir.resolve("rebasestate"));
  }

  private boolean isCherryPickInProgress() {
    return Files.exists(myHgDir.resolve("graftstate"));
  }

  @Nonnull
  public Repository.State readState() {
    if (isRebaseInProgress()) {
      return Repository.State.REBASING;
    }
    else if (isCherryPickInProgress()) {
      return Repository.State.GRAFTING;
    }
    return isMergeInProgress() ? Repository.State.MERGING : Repository.State.NORMAL;
  }

  public boolean isFresh() {
    return !Files.exists(myCacheDir);
  }

  private boolean branchExist() {
    return Files.exists(myCurrentBranch);
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> readBookmarks() {
    return readReferences(myBookmarksFile);
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> readTags() {
    return readReferences(myTagsFile);
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> readLocalTags() {
    return readReferences(myLocalTagsFile);
  }

  @Nonnull
  private Collection<HgNameWithHashInfo> readReferences(@Nonnull Path fileWithReferences) {
    HashSet<HgNameWithHashInfo> result = new HashSet<>();
    readReferences(fileWithReferences, result);
    return result;
  }

  private void readReferences(@Nonnull Path fileWithReferences, @Nonnull Collection<HgNameWithHashInfo> resultRefs) {
    // files like .hg/bookmarks which contains hash + name, f.e. 25e44c95b2612e3cdf29a704dabf82c77066cb67 A_BookMark or
    //files like .hg/patches/status hash:name, f.e. 25e44c95b2612e3cdf29a704dabf82c77066cb67:1.diff
    if (!Files.exists(fileWithReferences)) return;

    String[] namesWithHashes = DvcsUtil.tryLoadFileOrReturn(fileWithReferences, "").split("\n");
    for (String str : namesWithHashes) {
      Matcher matcher = HASH_NAME.matcher(str);
      if (matcher.matches()) {
        resultRefs.add(new HgNameWithHashInfo(matcher.group(2), myVcsObjectsFactory.createHash(matcher.group(1))));
      }
    }
  }

  @Nullable
  public String readCurrentBookmark() {
    return Files.exists(myCurrentBookmark) ? DvcsUtil.tryLoadFileOrReturn(myCurrentBookmark, "") : null;
  }

  @Nonnull
  public Collection<HgNameWithHashInfo> readSubrepos() {
    if (!hasSubrepos()) return Collections.emptySet();
    return readReferences(mySubrepoFile);
  }

  @Nonnull
  public List<HgNameWithHashInfo> readMQAppliedPatches() {
    ArrayList<HgNameWithHashInfo> mqPatchRefs = ContainerUtil.newArrayList();
    readReferences(myMqInternalDir.resolve("status"), mqPatchRefs);
    return mqPatchRefs;
  }

  @Nonnull
  public List<String> readMqPatchNames() {
    Path seriesFile = myMqInternalDir.resolve("series");
    return Files.exists(seriesFile) ? StringUtil.split(DvcsUtil.tryLoadFileOrReturn(seriesFile, ""), "\n") : List.of();
  }
}
