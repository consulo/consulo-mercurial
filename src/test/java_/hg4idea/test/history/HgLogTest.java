package hg4idea.test.history;

import static com.intellij.dvcs.test.Executor.cd;
import static com.intellij.dvcs.test.Executor.touch;
import static hg4idea.test.HgExecutor.hg;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.HgFileRevision;
import org.zmlx.hg4idea.command.HgLogCommand;
import org.zmlx.hg4idea.execution.HgCommandException;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import hg4idea.test.HgPlatformTest;

/**
 * @author Nadya Zabrodina
 */
public abstract class HgLogTest extends HgPlatformTest {

  public void testParseCopiedWithoutBraces() throws HgCommandException {
    parseCopied("f.txt");
  }

  public void testParseCopiedWithBraces() throws HgCommandException {
    parseCopied("(f.txt)");
  }

  private void parseCopied(@Nonnull String sourceFileName) throws HgCommandException {
    cd(myRepository);
    String copiedFileName = "copy".concat(sourceFileName);
    touch(sourceFileName);
    myRepository.refresh(false, true);
    hg("add " + sourceFileName);
    hg("commit -m a ");
    hg("cp " + sourceFileName + " " + copiedFileName);
    myRepository.refresh(false, true);
    hg("commit -m a ");
    HgLogCommand logCommand = new HgLogCommand(myProject);
    logCommand.setFollowCopies(false);
    VirtualFile copiedFile = myRepository.findChild(copiedFileName);
    assert copiedFile != null;
    final HgFile hgFile = new HgFile(myRepository, VfsUtilCore.virtualToIoFile(copiedFile));
    List<HgFileRevision> revisions = logCommand.execute(hgFile, 1, true);
    HgFileRevision rev = revisions.get(0);
    assertTrue(!rev.getAddedFiles().isEmpty());
  }
}
