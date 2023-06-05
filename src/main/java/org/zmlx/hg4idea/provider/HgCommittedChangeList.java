package org.zmlx.hg4idea.provider;

import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.versionBrowser.CommittedChangeListForRevision;
import org.zmlx.hg4idea.HgRevisionNumber;
import org.zmlx.hg4idea.HgVcs;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Date;

public class HgCommittedChangeList extends CommittedChangeListForRevision {

  @Nonnull
  private final HgVcs myVcs;
  @Nonnull
  private String myBranch;

  public HgCommittedChangeList(@Nonnull HgVcs vcs, @Nonnull HgRevisionNumber revision, @Nonnull String branch, String comment,
                               String committerName, Date commitDate, Collection<Change> changes) {
    super(revision.asString() + ": " + comment, comment, committerName, commitDate, changes, revision);
    myVcs = vcs;
    myBranch = StringUtil.isEmpty(branch) ? "default" : branch;
  }

  @Nonnull
  @Override
  public HgRevisionNumber getRevisionNumber() {
    return (HgRevisionNumber)super.getRevisionNumber();
  }

  @Nonnull
  public String getBranch() {
    return myBranch;
  }

  @Override
  public AbstractVcs getVcs() {
    return myVcs;
  }

  @Override
  public String toString() {
    return getComment();
  }
}
