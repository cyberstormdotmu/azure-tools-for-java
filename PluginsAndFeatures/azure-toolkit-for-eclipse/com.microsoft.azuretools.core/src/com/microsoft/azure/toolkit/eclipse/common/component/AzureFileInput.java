package com.microsoft.azure.toolkit.eclipse.common.component;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

public class AzureFileInput extends Composite implements AzureForm<String> {

    private AzureTextInput textInput;
    private Button button;

    public AzureFileInput(Composite parent, int style) {
        super(parent, style);
        setupUI();
    }

    public AzureTextInput getInput() {
        return textInput;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(textInput);
    }

    @Override
    public String getValue() {
        return textInput.getValue();
    }

    @Override
    public void setValue(String value) {
        textInput.setValue(value);
    }

    protected void setupUI() {
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        this.setLayout(gridLayout);

        textInput = new AzureTextInput(this, SWT.BORDER);
        textInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textInput.addValueChangedListener(value -> {
            AzureFileInput.this.fireValueChangedEvent(value);
        });

        button = new Button(this, SWT.PUSH);
        button.setText("Browse");
        button.setToolTipText("Select file from file system");
        button.setSize(92, textInput.getSize().y);
        button.addListener(SWT.Selection, event -> this.selectFile());
    }

    protected void selectFile() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setText("Choose file");
        dialog.setOverwrite(false);
        File origin = Optional.ofNullable(textInput.getValue()).map(File::new).orElse(null);
        if (origin != null && origin.exists()) {
            dialog.setFilterPath(origin.getParent().toString());
            dialog.setFileName(origin.getName());
        }
        final String result = dialog.open();
        if (StringUtils.isNotEmpty(result)) {
            textInput.setValue(result);
            AzureFileInput.this.fireValueChangedEvent(result);
        }
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

}
