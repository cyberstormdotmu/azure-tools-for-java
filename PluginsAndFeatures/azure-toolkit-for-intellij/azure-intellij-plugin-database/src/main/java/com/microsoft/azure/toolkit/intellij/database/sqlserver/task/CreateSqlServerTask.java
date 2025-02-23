/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.task;

import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerConfig;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import java.util.Collections;

public class CreateSqlServerTask {

    private final SqlServerConfig config;

    public CreateSqlServerTask(SqlServerConfig config) {
        this.config = config;
    }

    @AzureOperation(
        name = "sqlserver.create_server.server",
        params = {
            "config.getServerName()",
            "config.getSubscription().getName()"
        },
        type = AzureOperation.Type.SERVICE
    )
    public SqlServer execute() {
        final Operation operation = TelemetryManager.createOperation(ActionConstants.MySQL.CREATE);
        try {
            operation.start();
            final String subscriptionId = config.getSubscription().getId();
            EventUtil.logEvent(EventType.info, operation, Collections.singletonMap(TelemetryConstants.SUBSCRIPTIONID, subscriptionId));
            // create resource group if necessary.
            if (config.getResourceGroup() instanceof Draft) {
                ResourceGroup newResourceGroup = Azure.az(AzureGroup.class)
                    .subscription(subscriptionId).create(config.getResourceGroup().getName(), config.getRegion().getName());
                config.setResourceGroup(newResourceGroup);
            }

            // create sql server
            return Azure.az(AzureSqlServer.class).subscription(config.getSubscription().getId())
                    .create(com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerConfig.builder()
                    .name(config.getServerName()).subscription(config.getSubscription())
                    .resourceGroup(config.getResourceGroup()).region(config.getRegion())
                    .administratorLoginName(config.getAdminUsername())
                    .administratorLoginPassword(String.valueOf(config.getPassword()))
                    .enableAccessFromAzureServices(config.isAllowAccessFromAzureServices())
                    .enableAccessFromLocalMachine(config.isAllowAccessFromLocalMachine())
                    .build())
                    .commit();
        } catch (final RuntimeException e) {
            EventUtil.logError(operation, ErrorType.systemError, e, null, null);
            throw e;
        } finally {
            operation.complete();
        }
    }

}
