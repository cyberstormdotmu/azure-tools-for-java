/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import org.eclipse.swt.SWT;

import com.microsoft.azure.toolkit.eclipse.common.launch.AzureLaunchConfigurationTabGroup;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionDeployConfiguration;

public class AzureFunctionDeployTabGroup extends AzureLaunchConfigurationTabGroup {
    public AzureFunctionDeployTabGroup() {
        super("Run Azure Function", (parent) -> new AzureFunctionDeployComposite(parent, SWT.NONE), FunctionDeployConfiguration.class);
    }
}
