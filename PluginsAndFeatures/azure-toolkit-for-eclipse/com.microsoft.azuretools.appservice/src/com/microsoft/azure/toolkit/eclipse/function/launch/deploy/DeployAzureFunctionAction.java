/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.toolkit.eclipse.common.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;

public class DeployAzureFunctionAction {
    private static final String RUN = "run";
    private static final String DEPLOY_CONFIGURATION_TEMPLATE = "Deploy to Azure Function (%s)";
    private static final String DEPLOY_CONFIGURATION_TYPE_ID = "com.microsoft.azure.toolkit.eclipse.function.deployConfigurationType";

    public static void deployFunctionAppToAzure(@Nonnull final FunctionApp target) throws CoreException {
        final ILaunchConfigurationType deployType = getLaunchManager()
                .getLaunchConfigurationType(DEPLOY_CONFIGURATION_TYPE_ID);
        final ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations(deployType);
        final FunctionAppConfig targetConfig = FunctionAppConfig.fromRemote(target);
        final ILaunchConfiguration configuration = Arrays.stream(configs).filter(config -> {
            try {
                final FunctionDeployConfiguration deployConfiguration = LaunchConfigurationUtils
                        .getFromConfiguration(config, FunctionDeployConfiguration.class);
                final FunctionAppConfig configurationConfig = Optional.ofNullable(deployConfiguration)
                        .map(FunctionDeployConfiguration::getFunctionConfig).orElse(null);
                return FunctionAppConfig.isSameApp(configurationConfig, targetConfig);
            } catch (RuntimeException e) {
                return false;
            }
        }).findFirst().orElseGet(() -> createConfiguration(deployType, targetConfig));
        final int result = DebugUITools.openLaunchConfigurationPropertiesDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), configuration,
                DebugUITools.getLaunchGroup(configuration, RUN).getIdentifier(), null);
        if (result == Dialog.OK) {
            DebugUITools.launch(configuration, RUN);
        }
    }

    private static ILaunchConfiguration createConfiguration(ILaunchConfigurationType configType,
            FunctionAppConfig target) {
        ILaunchConfiguration config = null;
        ILaunchConfigurationWorkingCopy wc;
        try {
            wc = configType.newInstance(null,
                    getLaunchManager().generateLaunchConfigurationName(String.format(DEPLOY_CONFIGURATION_TEMPLATE, target.getName())));
            final FunctionDeployConfiguration configuration = new FunctionDeployConfiguration();
            configuration.setFunctionConfig(target);
            LaunchConfigurationUtils.saveToConfiguration(configuration, wc);
            config = wc.doSave();
        } catch (CoreException exception) {
            MessageDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), "Error",
                    exception.getStatus().getMessage());
        }
        return config;
    }

    private static ILaunchManager getLaunchManager() {
        return DebugPlugin.getDefault().getLaunchManager();
    }
}
