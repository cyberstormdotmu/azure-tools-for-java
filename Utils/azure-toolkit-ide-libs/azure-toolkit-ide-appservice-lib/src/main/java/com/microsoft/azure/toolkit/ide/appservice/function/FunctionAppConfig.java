/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.DraftServicePlan;
import com.microsoft.azure.toolkit.ide.common.model.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class FunctionAppConfig extends AppServiceConfig {
    public static final Runtime DEFAULT_RUNTIME = Runtime.FUNCTION_WINDOWS_JAVA8;
    @Builder.Default
    protected Runtime runtime = DEFAULT_RUNTIME;

    public static FunctionAppConfig getFunctionAppDefaultConfig() {
        return getFunctionAppDefaultConfig(StringUtils.EMPTY);
    }

    public static FunctionAppConfig getFunctionAppDefaultConfig(final String name) {
        final String appName = StringUtils.isEmpty(name) ? String.format("app-%s", DATE_FORMAT.format(new Date())) :
                String.format("app-%s-%s", name, DATE_FORMAT.format(new Date()));
        final Subscription subscription = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions().stream().findFirst().orElse(null);
        final DraftResourceGroup group = new DraftResourceGroup(subscription, StringUtils.substring(String.format("rg-%s", appName), 0, RG_NAME_MAX_LENGTH));
        group.setSubscription(subscription);
        final Region region = AppServiceConfig.getDefaultRegion();
        final String planName = StringUtils.substring(String.format("sp-%s", appName), 0, SP_NAME_MAX_LENGTH);
        final DraftServicePlan plan = new DraftServicePlan(subscription, planName, region, FunctionAppConfig.DEFAULT_RUNTIME.getOperatingSystem(),
                PricingTier.CONSUMPTION);
        return FunctionAppConfig.builder()
                .subscription(subscription)
                .resourceGroup(group)
                .name(appName)
                .servicePlan(plan)
                .runtime(FunctionAppConfig.DEFAULT_RUNTIME)
                .pricingTier(PricingTier.CONSUMPTION)
                .region(region).build();
    }

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> result = super.getTelemetryProperties();
        result.put("runtime", Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).map(OperatingSystem::getValue).orElse(StringUtils.EMPTY));
        result.put("functionJavaVersion", Optional.ofNullable(runtime).map(Runtime::getJavaVersion).map(JavaVersion::getValue).orElse(StringUtils.EMPTY));
        return result;
    }

    public static FunctionAppConfig fromRemote(FunctionApp functionApp) {
        return FunctionAppConfig.builder()
                .name(functionApp.name())
                .resourceId(functionApp.id())
                .servicePlan(AppServicePlanEntity.builder().id(functionApp.entity().getAppServicePlanId()).build())
                .subscription(Subscription.builder().id(functionApp.subscriptionId()).build())
                .resourceGroup(ResourceGroup.builder().name(functionApp.resourceGroup()).build())
                .runtime(functionApp.getRuntime())
                .region(functionApp.entity().getRegion())
                .appSettings(functionApp.entity().getAppSettings())
                .build();
    }
}
