/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBoxModel;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class WebAppComboBoxModel extends AppServiceComboBoxModel<WebApp, WebAppConfig> {

    private WebAppSettingModel webAppSettingModel;

    // todo: migrate Base Model to service library
    public WebAppComboBoxModel(final WebApp webApp) {
        super(webApp);
        this.runtime = webApp.getRuntime();
    }

    public WebAppComboBoxModel(WebAppSettingModel webAppSettingModel) {
        this.resourceId = webAppSettingModel.getWebAppId();
        // In case recover from configuration, get the app name from resource id
        this.appName =
                StringUtils.isEmpty(webAppSettingModel.getWebAppName()) && StringUtils.isNotEmpty(resourceId) ?
                        ResourceId.fromString(resourceId).name() :
                        webAppSettingModel.getWebAppName();
        this.resourceGroup = webAppSettingModel.getResourceGroup();
        this.subscriptionId = webAppSettingModel.getSubscriptionId();
        this.isNewCreateResource = webAppSettingModel.isCreatingNew();
        this.webAppSettingModel = webAppSettingModel;
        this.runtime = webAppSettingModel.getRuntime();
    }
}
