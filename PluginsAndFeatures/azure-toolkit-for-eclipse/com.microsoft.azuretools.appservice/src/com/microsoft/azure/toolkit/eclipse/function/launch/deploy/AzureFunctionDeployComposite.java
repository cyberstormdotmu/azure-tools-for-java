/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.eclipse.common.form.AzureFormPanel;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.ui.AzureFunctionComboBox;
import com.microsoft.azure.toolkit.eclipse.function.ui.FunctionProjectComboBox;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.jdt.core.IJavaProject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AzureFunctionDeployComposite extends Composite implements AzureFormPanel<FunctionDeployConfiguration> {
    private FunctionProjectComboBox cbProject;
    private AzureFileInput txtFunctionCli;
    private AzureFunctionComboBox azureFunctionComboBox;
    private AzureFileInput txtLocalSettingsJson;

    /**
     * Create the composite.
     *
     * @param parent
     * @param style
     */
    public AzureFunctionDeployComposite(Composite parent, int style) {
        super(parent, style);
        setupUI();
    }

    private void setupUI() {
        GridLayoutFactory.swtDefaults().numColumns(2).applyTo(this);
        this.setLayout(new GridLayout(2, false));

        Label lblProject = new Label(this, SWT.NONE);
        lblProject.setText("Project:");
        GridDataFactory.swtDefaults().applyTo(lblProject);

        cbProject = new FunctionProjectComboBox(this);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(cbProject);
        cbProject.setLabeledBy(lblProject);
        cbProject.setRequired(true);
        cbProject.addValueChangedListener(this::setLocalSettingsJsonByProject);

        Label lblFunction = new Label(this, SWT.NONE);
        lblFunction.setText("Function:");

        azureFunctionComboBox = new AzureFunctionComboBox(this);
        azureFunctionComboBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        azureFunctionComboBox.setLabeledBy(lblFunction);
        azureFunctionComboBox.setRequired(true);

        Label lblFunctionCli = new Label(this, SWT.NONE);
        lblFunctionCli.setText("Function CLI:");

        txtFunctionCli = new AzureFileInput(this, SWT.NONE);
        txtFunctionCli.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtFunctionCli.setRequired(true);
        txtFunctionCli.setLabeledBy(lblFunctionCli);

        Label lblLocalSettings = new Label(this, SWT.NONE);
        lblLocalSettings.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblLocalSettings.setText("Local settings:");

        txtLocalSettingsJson = new AzureFileInput(this, SWT.NONE);
        txtLocalSettingsJson.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtLocalSettingsJson.setRequired(false);
        txtLocalSettingsJson.setLabeledBy(lblLocalSettings);

        cbProject.refreshItems();
        azureFunctionComboBox.refreshItems();
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtFunctionCli, txtLocalSettingsJson, cbProject, azureFunctionComboBox);
    }

    @Override
    public FunctionDeployConfiguration getValue() {
        FunctionDeployConfiguration config = new FunctionDeployConfiguration();
        config.setProjectName(Optional.ofNullable(cbProject.getValue()).map(IJavaProject::getElementName).orElse(null));
        config.setFunctionCliPath(txtFunctionCli.getValue());
        config.setLocalSettingsJsonPath(txtLocalSettingsJson.getValue());
        config.setFunctionConfig(azureFunctionComboBox.getValue());
        return config;
    }

    @Override
    public void setValue(FunctionDeployConfiguration config) {
        // Set local settings json first in case it was replaced by project listener
        this.txtLocalSettingsJson.setValue(config.getLocalSettingsJsonPath());
        if (StringUtils.isNotBlank(config.getProjectName())) {
            this.cbProject
                    .setValue(new AzureComboBox.ItemReference<>(config.getProjectName(), IJavaElement::getElementName));
        }
        if (StringUtils.isNotBlank(config.getFunctionCliPath())) {
            txtFunctionCli.setValue(config.getFunctionCliPath());
        } else {
            try {
                txtFunctionCli.setValue(FunctionUtils.getFuncPath());
            } catch (IOException | InterruptedException e) {
                AzureMessager.getMessager().warning("Cannot find function core tools due to error:" + e.getMessage());
            }
        }
        Optional.ofNullable(config.getFunctionConfig()).ifPresent(functionAppConfig -> {
            if (StringUtils.isEmpty(functionAppConfig.getResourceId())) {
                azureFunctionComboBox.setConfigModel(functionAppConfig);
            }
            azureFunctionComboBox.setValue(new AzureComboBox.ItemReference<>(item -> FunctionAppConfig.isSameApp(item, functionAppConfig)));
            azureFunctionComboBox.refreshItems();
        });
    }

    private void setLocalSettingsJsonByProject(final IJavaProject project) {
        if (project == null) {
            return;
        }
        final IFile file = project.getProject().getFile("local.settings.json");
        if (file.exists() && !this.txtLocalSettingsJson.isUserInput()) {
            this.txtLocalSettingsJson.setValue(file.getLocation().toFile().toString());
        }
    }
}
