package consulo.mercurial;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.plain.PlainTextFileType;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.zmlx.hg4idea.repo.HgRepositoryFiles;

/**
 * @author VISTALL
 * @since 2025-08-31
 */
@ExtensionImpl
public class HgFileTypeFactory extends FileTypeFactory {
    private final FileNameMatcherFactory myFileNameMatcherFactory;

    @Inject
    public HgFileTypeFactory(FileNameMatcherFactory fileNameMatcherFactory) {
        myFileNameMatcherFactory = fileNameMatcherFactory;
    }

    @Override
    public void createFileTypes(@Nonnull FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(PlainTextFileType.INSTANCE, myFileNameMatcherFactory.createExactFileNameMatcher(HgRepositoryFiles.HGIGNORE));
    }
}
