/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionAndResourceGroupComposite;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

public class AppServiceCreationComposite<T extends AppServiceConfig> extends Composite implements AzureForm<T> {
    private Supplier<T> supplier;
    private AppServiceInstanceDetailComposite instanceDetailPanel;
    private SubscriptionAndResourceGroupComposite subsAndResourceGroupPanel;
    private AppServicePlanComposite appServicePlanPanel;

    public AppServiceCreationComposite(Composite parent, int style, Supplier<T> defaultConfigSupplier) {
        super(parent, style);
        this.supplier = defaultConfigSupplier;
        setupUI();
    }

    private void setupUI() {
        setLayout(new GridLayout(1, false));
        Group grpProjectDetails = new Group(this, SWT.NONE);
        grpProjectDetails.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpProjectDetails.setText("Project Details");
        grpProjectDetails.setLayout(new FillLayout(SWT.HORIZONTAL));

        subsAndResourceGroupPanel = new SubscriptionAndResourceGroupComposite(grpProjectDetails, SWT.NONE);

        Group grpInstanceDetails = new Group(this, SWT.NONE);
        grpInstanceDetails.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpInstanceDetails.setText("Instance Details");
        grpInstanceDetails.setLayout(new FillLayout(SWT.HORIZONTAL));
        instanceDetailPanel = new AppServiceInstanceDetailComposite(grpInstanceDetails, SWT.NONE);

        Group grpAppServicePlan = new Group(this, SWT.NONE);
        grpAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpAppServicePlan.setText("App Service Plan");
        grpAppServicePlan.setLayout(new FillLayout(SWT.HORIZONTAL));

        appServicePlanPanel = new AppServicePlanComposite(grpAppServicePlan, SWT.NONE);

        SubscriptionComboBox subscriptionComboBox = subsAndResourceGroupPanel.getSubscriptionComboBox();
        subscriptionComboBox.addValueChangedListener(event -> {
            Subscription subs = subscriptionComboBox.getValue();
            instanceDetailPanel.setSubscription(subs);
            instanceDetailPanel.getRegionComboBox().setSubscription(subs);
            appServicePlanPanel.getServicePlanCombobox().setSubscription(subs);
        });
        instanceDetailPanel.getRegionComboBox().addValueChangedListener(event -> {
            Region region = instanceDetailPanel.getRegionComboBox().getValue();
            appServicePlanPanel.getServicePlanCombobox().setAzureRegion(region);
        });

        instanceDetailPanel.getRuntimeComboBox().addValueChangedListener(event -> {
            Runtime r = instanceDetailPanel.getRuntimeComboBox().getValue();
            if (r != null) {
                appServicePlanPanel.getServicePlanCombobox().setOs(r.getOperatingSystem());
            }

        });
        subscriptionComboBox.refreshItems();
    }

    public void setRuntime(final List<Runtime> platformList) {
        instanceDetailPanel.getRuntimeComboBox().setPlatformList(platformList);
    }

    @Override
    protected void checkSubclass() {
    }

    @Override
    public T getValue() {
        final T result = supplier.get();
        final RuntimeConfig runtime = instanceDetailPanel.getRuntime();
        result.setSubscription(subsAndResourceGroupPanel.getSubscription());
        result.setResourceGroup(subsAndResourceGroupPanel.getResourceGroup());
        result.setName(instanceDetailPanel.getAppName());
        result.setRegion(instanceDetailPanel.getResourceRegion());
        result.setRuntime(Runtime.getRuntime(runtime.os(), runtime.webContainer(), runtime.javaVersion()));
        result.setServicePlan(appServicePlanPanel.getServicePlan());
        result.setPricingTier(Optional.ofNullable(appServicePlanPanel.getServicePlan())
                .map(AppServicePlanEntity::getPricingTier).orElse(null));
        result.setAppSettings(new HashMap<>());
        return result;
    }

    @Override
    public void setValue(T config) {
        Optional.ofNullable(config.getSubscription()).ifPresent(
                subscription -> subsAndResourceGroupPanel.getSubscriptionComboBox().setValue(new ItemReference<>(
                        value -> StringUtils.equalsIgnoreCase(value.getId(), subscription.getId()))));
        Optional.ofNullable(config.getResourceGroup()).ifPresent(
                resourceGroup -> subsAndResourceGroupPanel.getResourceGroupComboBox().setValue(resourceGroup));
        Optional.ofNullable(config.getName())
                .ifPresent(name -> instanceDetailPanel.getWebAppNameInput().setValue(name));
        Optional.ofNullable(config.getRegion())
                .ifPresent(region -> instanceDetailPanel.getRegionComboBox().setValue(region));
        Optional.ofNullable(config.getRuntime())
                .ifPresent(runtime -> instanceDetailPanel.getRuntimeComboBox().setValue(runtime));
        Optional.ofNullable(config.getServicePlan())
                .ifPresent(servicePlan -> appServicePlanPanel.getServicePlanCombobox().setValue(servicePlan));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Stream.of(subsAndResourceGroupPanel.getInputs(), instanceDetailPanel.getInputs(),
                appServicePlanPanel.getInputs()).flatMap(List::stream).collect(Collectors.toList());
    }
}
