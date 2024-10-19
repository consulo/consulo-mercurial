package org.zmlx.hg4idea.ui;

import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.UIUtil;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgBranchReferenceValidator;
import org.zmlx.hg4idea.util.HgReferenceValidator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

import static consulo.ui.ex.awt.UIUtil.DEFAULT_HGAP;
import static consulo.ui.ex.awt.UIUtil.DEFAULT_VGAP;

public class HgBookmarkDialog extends DialogWrapper {
    @Nonnull
    private HgRepository myRepository;
    @Nonnull
    private JBTextField myBookmarkName;
    @Nonnull
    private JBCheckBox myActiveCheckbox;

    public HgBookmarkDialog(@Nonnull HgRepository repository) {
        super(repository.getProject(), false);
        myRepository = repository;
        setTitle("Create Bookmark");
        setResizable(false);
        init();
    }

    @Override
    @Nullable
    protected String getHelpId() {
        return "reference.mercurial.create.bookmark";
    }

    @Override
    @Nonnull
    public JComponent getPreferredFocusedComponent() {
        return myBookmarkName;
    }

    @Override
    @Nonnull
    protected String getDimensionServiceKey() {
        return HgBookmarkDialog.class.getName();
    }

    @Override
    @Nonnull
    protected JComponent createCenterPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBag g = new GridBag()
            .setDefaultInsets(new Insets(0, 0, DEFAULT_VGAP, DEFAULT_HGAP))
            .setDefaultAnchor(GridBagConstraints.LINE_START)
            .setDefaultFill(GridBagConstraints.HORIZONTAL);

        JLabel icon = new JBLabel(UIUtil.getQuestionIcon(), SwingConstants.LEFT);
        myBookmarkName = new JBTextField(13);
        myBookmarkName.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void textChanged(DocumentEvent e) {
                validateFields();
            }
        });

        JBLabel bookmarkLabel = new JBLabel("Bookmark name:");
        bookmarkLabel.setLabelFor(myBookmarkName);

        myActiveCheckbox = new JBCheckBox("Inactive", false);

        contentPanel.add(icon, g.nextLine().next().coverColumn(3).pady(DEFAULT_HGAP));
        contentPanel.add(bookmarkLabel, g.next().fillCellNone().insets(new Insets(0, 6, DEFAULT_VGAP, DEFAULT_HGAP)));
        contentPanel.add(myBookmarkName, g.next().coverLine().setDefaultWeightX(1));
        contentPanel.add(myActiveCheckbox, g.nextLine().next().next().coverLine(2));
        return contentPanel;
    }

    private void validateFields() {
        HgReferenceValidator validator = new HgBranchReferenceValidator(myRepository);
        String name = getName();
        if (!validator.checkInput(name)) {
            String message = validator.getErrorText(name);
            setErrorText(message == null ? "You have to specify bookmark name." : message);
            setOKActionEnabled(false);
            return;
        }
        setErrorText(null);
        setOKActionEnabled(true);
    }

    public boolean isActive() {
        return !myActiveCheckbox.isSelected();
    }

    @Nullable
    public String getName() {
        return myBookmarkName.getText();
    }
}
