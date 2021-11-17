/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.microsoft.azure.toolkit.eclipse.appservice.serviceplan.DraftServicePlan;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionAndResourceGroupComposite;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.eclipse.common.component.resourcegroup.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.eclipse.swt.layout.GridLayout;

public class AppServiceCreationComposite<T extends AppServiceConfig> extends Composite implements AzureForm<T> {
    private Supplier<T> supplier;
    private AppServiceInstanceDetailComposite instanceDetailPanel;
    private SubscriptionAndResourceGroupComposite subsAndResourceGroupPanel;
    private AppServicePlanComposite appServicePlanPanel;
        
    public AppServiceCreationComposite(Composite parent, int style,  Supplier<T> supplier) {
        super(parent, style);
        this.supplier = supplier;
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
        final AppServicePlanEntity entity = appServicePlanPanel.getServicePlan();
        final T result = supplier.get();
        result.subscriptionId(subsAndResourceGroupPanel.getSubscription().getId());
        result.resourceGroup(subsAndResourceGroupPanel.getResourceGroup().getName());
        result.appName(instanceDetailPanel.getAppName());
        result.region(instanceDetailPanel.getResourceRegion());
        result.runtime(instanceDetailPanel.getRuntime());
        result.servicePlanName(entity.getName());
        result.servicePlanResourceGroup(StringUtils.firstNonBlank(entity.getResourceGroup(),
                subsAndResourceGroupPanel.getResourceGroup().getName()));
        result.pricingTier(entity.getPricingTier());
        result.appSettings(new HashMap<>());
        return result;
    }

    @Override
    public void setValue(T config) {
        Optional.ofNullable(config.subscriptionId()).ifPresent(
                subscription -> subsAndResourceGroupPanel.getSubscriptionComboBox().setValue(new ItemReference<>(
                        value -> StringUtils.equalsIgnoreCase(value.getId(), config.subscriptionId()))));
        Optional.ofNullable(config.resourceGroup()).ifPresent(resourceGroup -> subsAndResourceGroupPanel
                .getResourceGroupComboBox().setValue(new DraftResourceGroup(resourceGroup)));
        Optional.ofNullable(config.appName())
                .ifPresent(name -> instanceDetailPanel.getWebAppNameInput().setValue(name));
        Optional.ofNullable(config.region())
                .ifPresent(region -> instanceDetailPanel.getRegionComboBox().setValue(region));
        Optional.ofNullable(config.runtime())
                .ifPresent(runtime -> instanceDetailPanel.getRuntimeComboBox()
                        .setValue(new ItemReference<>(value -> Objects.equals(runtime.os(), value.getOperatingSystem())
                                && Objects.equals(runtime.webContainer(), value.getWebContainer())
                                && Objects.equals(runtime.javaVersion(), value.getJavaVersion()))));
        Optional.ofNullable(config.getServicePlanConfig())
                .ifPresent(servicePlan -> appServicePlanPanel.getServicePlanCombobox()
                        .setValue(new DraftServicePlan(servicePlan.servicePlanName(), servicePlan.pricingTier())));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Stream.of(subsAndResourceGroupPanel.getInputs(), instanceDetailPanel.getInputs(),
                appServicePlanPanel.getInputs()).flatMap(List::stream).collect(Collectors.toList());
    }
}
