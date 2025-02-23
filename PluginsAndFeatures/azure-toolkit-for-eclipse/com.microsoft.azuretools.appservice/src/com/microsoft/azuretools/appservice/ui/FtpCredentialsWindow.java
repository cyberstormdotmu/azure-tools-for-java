/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.appservice.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class FtpCredentialsWindow extends AzureTitleAreaDialogWrapper {

    private Text textHost;
    private Text textUsername;
    private Text textPassword;
    private WebApp webApp;

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    public FtpCredentialsWindow(Shell parentShell, WebApp webApp) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        setHelpAvailable(false);
        this.webApp = webApp;
        showFtpCredentials();
    }

    /**
     * Create contents of the dialog.
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("FTP Credentials");
        setMessage("Web App '" + webApp.name() + "' FTP deployment server credentials.");

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label lblNewLabel = new Label(container, SWT.NONE);
        lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblNewLabel.setText("Host:");

        textHost = new Text(container, SWT.BORDER);
        textHost.setEditable(false);
        textHost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textHost.setText("Loading...");

        Label lblUserName = new Label(container, SWT.NONE);
        lblUserName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblUserName.setText("Username:");

        textUsername = new Text(container, SWT.BORDER);
        textUsername.setEditable(false);
        textUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textUsername.setText("Loading...");

        Label lblPassword = new Label(container, SWT.NONE);
        lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblPassword.setText("Password:");

        textPassword = new Text(container, SWT.BORDER);
        textPassword.setEditable(false);
        textPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textPassword.setText("Loading...");

        return area;
    }

    /**
     * Create contents of the button bar.
     *
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        button.setText("Close");
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(520, 246);
    }

    private void showFtpCredentials() {
        Mono.fromCallable(webApp::getPublishingProfile).subscribeOn(Schedulers.boundedElastic()).subscribe(profile -> {
            AzureTaskManager.getInstance().runLater(() -> {
                textHost.setText(profile.getFtpUrl());
                textUsername.setText(profile.getFtpUsername());
                textPassword.setText(profile.getFtpPassword());
            });
        });
    }
}
