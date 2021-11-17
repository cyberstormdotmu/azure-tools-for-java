/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp.creation;

import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.eclipse.appservice.AppServiceCreationComposite;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureDialog;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class CreateWebAppDialog extends AzureDialog<AppServiceConfig> implements AzureForm<AppServiceConfig> {
    private AppServiceConfig config;
    private AppServiceCreationComposite composite;

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    public CreateWebAppDialog(Shell parentShell, AppServiceConfig config) {
        super(parentShell);
        this.config = config;
        setShellStyle(SWT.SHELL_TRIM);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        this.composite = new AppServiceCreationComposite(container, SWT.NONE);
        this.composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        return container;
    }

    @Override
    public AppServiceConfig getValue() {
        return this.composite.getValue();
    }

    @Override
    public void setValue(AppServiceConfig config) {
        this.composite.setValue(config);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return this.composite.getInputs();
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Web App";
    }

    @Override
    public AzureForm<AppServiceConfig> getForm() {
        return this;
    }

    @Override
    public int open() {
        Optional.ofNullable(config).ifPresent(config -> AzureTaskManager.getInstance().runLater(() -> setValue(config)));
        return super.open();
    }

}
