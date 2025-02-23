/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class VersionComboBox extends AzureComboBox<String> {

    @NotNull
    @Override
    @AzureOperation(
        name = "mysql.list_versions",
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends String> loadItems() {
        return Azure.az(AzureMySql.class).listSupportedVersions();
    }
}
