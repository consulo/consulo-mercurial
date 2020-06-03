package org.zmlx.hg4idea.provider;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcs.CommittedChangeListForRevision;
import javax.annotation.Nonnull;
import org.zmlx.hg4idea.HgRevisionNumber;
import org.zmlx.hg4idea.HgVcs;

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
