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

import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;


/**
 * Stores paths to Hg service files (from .hg/ directory) that are used by IDEA, and provides methods to check if a file
 * matches once of them.
 *
 */
public class HgRepositoryFiles {

  private static final String BRANCHHEADS = "cache/branch";//branchheads <2.5; branchheads-served >= 2.5 and <2.9; branch2-served >=2.9
  // so check for starting branch
  private static final String BRANCHEADSDIR = "cache";
  private static final String MERGE = "merge";
  private static final String REBASE = "rebase"; //rebasestate
  private static final String BRANCH = "branch";
  private static final String DIRSTATE = "dirstate";
  private static final String BOOKMARKS = "bookmarks";
  private static final String LOCAL_TAGS = "localtags";
  private static final String TAGS = ".hgtags";
  private static final String CURRENT_BOOKMARK = "bookmarks.current";
  private static final String MQDIR = "patches";
  private static final String CONFIG_HGRC = "hgrc";
  private static final String HGIGNORE = ".hgignore";


  @Nonnull
  private final String myBranchHeadsPath;
  @Nonnull
  private final String myBranchHeadsDirPath;
  @Nonnull
  private final String myMergePath;
  @Nonnull
  private final String myRebasePath;
  @Nonnull
  private final String myBranchPath;
  @Nonnull
  private final String myDirstatePath;
  @Nonnull
  private final String myBookmarksPath;
  @Nonnull
  private final String myTagsPath;
  @Nonnull
  private final String myLocalTagsPath;
  @Nonnull
  private final String myCurrentBookmarkPath;
  @Nonnull
  private final String myMQDirPath;
  @Nonnull
  private final String myConfigHgrcPath;
  @Nonnull
  private final String myHgIgnorePath;

  @Nonnull
  public static HgRepositoryFiles getInstance(@Nonnull VirtualFile hgDir) {
    return new HgRepositoryFiles(hgDir);
  }

  private HgRepositoryFiles(@Nonnull VirtualFile hgDir) {
    myBranchHeadsPath = hgDir.getPath() + slash(BRANCHHEADS);
    myBranchHeadsDirPath = hgDir.getPath() + slash(BRANCHEADSDIR);
    myBranchPath = hgDir.getPath() + slash(BRANCH);
    myDirstatePath = hgDir.getPath() + slash(DIRSTATE);
    myMergePath = hgDir.getPath() + slash(MERGE);
    myRebasePath = hgDir.getPath() + slash(REBASE);
    myBookmarksPath = hgDir.getPath() + slash(BOOKMARKS);
    VirtualFile repoDir = hgDir.getParent();
    myTagsPath = repoDir.getPath() + slash(TAGS);
    myLocalTagsPath = hgDir.getPath() + slash(LOCAL_TAGS);
    myCurrentBookmarkPath = hgDir.getPath() + slash(CURRENT_BOOKMARK);
    myMQDirPath = hgDir.getPath() + slash(MQDIR);
    myConfigHgrcPath = hgDir.getPath() + slash(CONFIG_HGRC);
    myHgIgnorePath = repoDir.getPath() + slash(HGIGNORE);
  }

  @Nonnull
  private static String slash(@Nonnull String s) {
    return "/" + s;
  }

  /**
   * Returns subdirectories of .hg which we are interested in - they should be watched by VFS.
   */
  @Nonnull
  static Collection<String> getSubDirRelativePaths() {
    return Arrays.asList(slash(BRANCHHEADS), slash(MERGE));
  }

  @Nonnull
  public String getBranchHeadsDirPath() {
    return myBranchHeadsDirPath;
  }

  @Nonnull
  public String getMQDirPath() {
    return myMQDirPath;
  }

  public boolean isbranchHeadsFile(String filePath) {
    return filePath.startsWith(myBranchHeadsPath);
  }

  public boolean isBranchFile(String filePath) {
    return filePath.equals(myBranchPath);
  }

  public boolean isDirstateFile(String filePath) {
    return filePath.equals(myDirstatePath);
  }

  public boolean isMergeFile(String filePath) {
    return filePath.startsWith(myMergePath);
  }

  public boolean isRebaseFile(String filePath) {
    return filePath.startsWith(myRebasePath);
  }

  public boolean isBookmarksFile(String filePath) {
    return filePath.equals(myBookmarksPath);
  }

  public boolean isCurrentBookmarksFile(String filePath) {
    return filePath.equals(myCurrentBookmarkPath);
  }

  public boolean isConfigHgrcFile(String filePath) {
    return filePath.equals(myConfigHgrcPath);
  }

  public boolean isTagsFile(String filePath) {
    return filePath.equals(myTagsPath);
  }

  public boolean isLocalTagsFile(String filePath) {
    return filePath.equals(myLocalTagsPath);
  }

  public boolean isMqFile(String filePath) {
    return filePath.startsWith(myMQDirPath);
  }

  public boolean isHgIgnore(String filePath) {
    return filePath.equals(myHgIgnorePath);
  }
}
