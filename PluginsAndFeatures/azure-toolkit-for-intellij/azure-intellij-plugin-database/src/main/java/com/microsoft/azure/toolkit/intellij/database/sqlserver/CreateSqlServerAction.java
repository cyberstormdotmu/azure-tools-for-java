/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.creation.SqlServerCreationDialog;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.task.CreateSqlServerTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerConfig;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerModule;

@Name("Create")
public class CreateSqlServerAction extends NodeActionListener {

    private final SqlServerModule model;

    public CreateSqlServerAction(SqlServerModule model) {
        super();
        this.model = model;
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final Project project = (Project) model.getProject();
        AzureLoginHelper.requireSignedIn(project, () -> this.doActionPerformed(project));
    }

    private void doActionPerformed(Project project) {
        final SqlServerCreationDialog dialog = new SqlServerCreationDialog(project);
        dialog.setOkActionListener((data) -> this.createSqlServer(data, dialog, project));
        dialog.show();
    }

    private void createSqlServer(final SqlServerConfig config, final SqlServerCreationDialog dialog, final Project project) {
        final Runnable runnable = () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            DefaultLoader.getIdeHelper().invokeLater(dialog::close);
            new CreateSqlServerTask(config).execute();
        };
        String progressMessage = Node.getProgressMessage(AzureActionEnum.CREATE.getDoingName(), SqlServerModule.MODULE_NAME, config.getServerName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, progressMessage, false, runnable));
    }

}
