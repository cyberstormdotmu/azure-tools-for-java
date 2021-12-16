/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaProject;

import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.eclipse.common.launch.AzureLongDurationTaskRunnerWithConsole;
import com.microsoft.azure.toolkit.eclipse.common.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.jdt.EclipseFunctionProject;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.util.JsonUtils;
import com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionPackager;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

public class AzureFunctionDeployLaunchDelegate extends LaunchConfigurationDelegate {
    private static final String LOCAL_SETTINGS_VALUES = "Values";

    @Override
    @AzureOperation(name = "function.deploy.configuration", type = AzureOperation.Type.ACTION)
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        final FunctionDeployConfiguration config = LaunchConfigurationUtils.getFromConfiguration(configuration,
                FunctionDeployConfiguration.class);
        final String projectName = config.getProjectName();
        final IJavaProject project = Arrays.stream(FunctionUtils.listFunctionProjects())
                .filter(t -> StringUtils.equals(t.getElementName(), projectName)).findFirst().orElse(null);
        if (project == null) {
            AzureMessager.getMessager().error("Cannot find the specified project:" + projectName);
            throw new AzureToolkitRuntimeException("Cannot find the specified project:" + projectName);
        }
        final FunctionAppConfig functionConfig = config.getFunctionConfig();
        AzureLongDurationTaskRunnerWithConsole.getInstance().runTask(AzureString
                .format("Deploy project %s to function app %s", projectName, functionConfig.getName()).toString(),
                () -> {
                    final File tempFolder = FunctionUtils.getTempStagingFolder().toFile();
                    try {
                        // build staging folder
                        FunctionUtils.buildMavenProject(project);
                        FileUtils.cleanDirectory(tempFolder);
                        final File file = project.getProject().getFile("host.json").getLocation().toFile();
                        final EclipseFunctionProject eclipseFunctionProject = new EclipseFunctionProject(project,
                                tempFolder);
                        eclipseFunctionProject.setHostJsonFile(file);
                        eclipseFunctionProject.buildJar();
                        AzureFunctionPackager.getInstance().packageProject(eclipseFunctionProject, true,
                                config.getFunctionCliPath());
                        FileUtils.deleteQuietly(eclipseFunctionProject.getArtifactFile());
                        // create or update function app
                        // todo: read local.settings.json and update app settings
                        final com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig taskConfig = FunctionAppConfig
                                .convertToTaskConfig(functionConfig);
                        final IFunctionAppBase<?> function = new CreateOrUpdateFunctionAppTask(taskConfig).execute();
                        // deploy function app
                        new DeployFunctionAppTask(function, tempFolder, null).execute();
                    } catch (Throwable e) {
                        AzureMessager.getMessager().error(e);
                    }
                }, false);
    }
    
    public static Map<String, String> getAppSettingsFromLocalSettingsJson(File target) {
        final Map<String, String> result = new HashMap<>();
        final JsonObject jsonObject = JsonUtils.readJsonFile(target);
        if (jsonObject == null) {
            return new HashMap<>();
        }
        final JsonObject valueObject = jsonObject.getAsJsonObject(LOCAL_SETTINGS_VALUES);
        valueObject.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        return result;
    }
}
