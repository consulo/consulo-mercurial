package org.zmlx.hg4idea.test;

import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsHistorySession;
import consulo.versionControlSystem.util.VcsUtil;
import org.junit.Test;
import org.zmlx.hg4idea.HgVcs;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * HgHistoryTest tests retrieving file history and specific revisions.
 */
public abstract class HgHistoryTest extends HgSingleUserTest {

  /**
   * 1. Make two versions of a file (create, add, commit, modify, commit).
   * 2. Get the revisions history.
   * 3. Verify versions' contents and the current version.
   */
  @Test
  public void testCurrentAndPreviousRevisions() throws Exception {
    int versions = 0;
    fillFile(myProjectDir, new String[]{ AFILE }, INITIAL_FILE_CONTENT);
    addAll();
    commitAll("initial content");
    versions++;
    fillFile(myProjectDir, new String[] { AFILE} , UPDATED_FILE_CONTENT);
    commitAll("updated content");
    versions++;

    final VcsHistorySession session = getHistorySession(AFILE);
    final List<VcsFileRevision> revisions = session.getRevisionList();
    for (VcsFileRevision rev : revisions) {
      rev.loadContent();
    }

    assertEquals(revisions.size(), versions);
    assertTrue(session.isCurrentRevision(revisions.get(0).getRevisionNumber()));
    assertEquals(revisions.get(0).getContent(), UPDATED_FILE_CONTENT.getBytes());
    assertEquals(revisions.get(1).getContent(), INITIAL_FILE_CONTENT.getBytes());
  }

  /**
   * 1. Make initial version of a file (create, add, commit).
   * 2. Rename file (rename, commit).
   * 3. Update file (modify, commit).
   * 4. Get the file history.
   * 5. Verify revision contents and the current revision.
   */
  @Test
  public void renameShouldPreserveFileHistory() throws Exception {
    int versions = 0;

    fillFile(myProjectDir, new String[]{ AFILE }, INITIAL_FILE_CONTENT);
    addAll();
    commitAll("initial content");
    versions++;

    runHgOnProjectRepo("rename", AFILE, BFILE);
    commitAll("file renamed");
    versions++;

    fillFile(myProjectDir, new String[]{ BFILE }, UPDATED_FILE_CONTENT);
    commitAll("updated content");
    versions++;

    final VcsHistorySession session = getHistorySession(BFILE);
    final List<VcsFileRevision> revisions = session.getRevisionList();
    loadAllRevisions(revisions);

    assertEquals(revisions.size(), versions);
    assertTrue(session.isCurrentRevision(revisions.get(0).getRevisionNumber()));
    assertEquals(revisions.get(0).getContent(), UPDATED_FILE_CONTENT.getBytes());
    assertEquals(revisions.get(2).getContent(), INITIAL_FILE_CONTENT.getBytes());
  }
  
  @Test
  public void locallyRenamedFileShouldGetHistory() throws Exception {
    int versions = 0;
    fillFile(myProjectDir, new String[]{ AFILE }, INITIAL_FILE_CONTENT);
    addAll();
    commitAll("initial content");
    versions++;
    fillFile(myProjectDir, new String[]{AFILE}, UPDATED_FILE_CONTENT);
    commitAll("updated content");
    versions++;

    
    runHgOnProjectRepo("rename", AFILE, BFILE);
    //don't commit 

    refreshVfs();
    ChangeListManager.getInstance(myProject).ensureUpToDate(false);
    
    final VcsHistorySession session = getHistorySession(BFILE);
    final List<VcsFileRevision> revisions = session.getRevisionList();
    for (VcsFileRevision rev : revisions) {
      rev.loadContent();
    }

    assertEquals(revisions.size(), versions);
    assertTrue(session.isCurrentRevision(revisions.get(0).getRevisionNumber()));
    assertEquals(revisions.get(0).getContent(), UPDATED_FILE_CONTENT.getBytes());
    assertEquals(revisions.get(1).getContent(), INITIAL_FILE_CONTENT.getBytes());
    
  }

  private static void loadAllRevisions(Collection<VcsFileRevision> revisions) throws Exception {
    for (VcsFileRevision rev : revisions) {
      rev.loadContent();
    }
  }

  private VcsHistorySession getHistorySession(String relativePath) throws VcsException {
    return HgVcs.getInstance(myProject).getVcsHistoryProvider().createSessionFor(VcsUtil.getFilePath(new File(myProjectDir, relativePath), false));
  }

}
