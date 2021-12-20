/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.common.component.resourcegroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.ide.common.model.Draft;
import com.microsoft.azure.toolkit.ide.common.model.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;

public class ResourceGroupComboBox extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;
    private final List<ResourceGroup> draftItems = new ArrayList<>();

    public ResourceGroupComboBox(Composite parent) {
        super(parent);
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }

        final ResourceGroup entity = (ResourceGroup) item;
        if (item instanceof Draft) {
            return "(New) " + entity.getName();
        }
        return entity.getName();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @Nonnull
    @Override
    @AzureOperation(
            name = "arm|rg.list.subscription", //TODO: message bundle
            params = {"this.subscription.getId()"},
            type = AzureOperation.Type.SERVICE
    )
    protected List<? extends ResourceGroup> loadItems() {
        final List<ResourceGroup> groups = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            groups.addAll(this.draftItems);
        }
        if (Objects.nonNull(this.subscription)) {
            final String sid = subscription.getId();
            final List<ResourceGroup> remoteGroups = Azure.az(AzureGroup.class).list(sid);
            remoteGroups.sort(Comparator.comparing(ResourceGroup::getName));
            groups.addAll(remoteGroups);
        }
        return groups;
    }

    protected Control getExtension() {
        Button button = new Button(this, SWT.NONE);
        button.setText("Create");
        button.setToolTipText("Create new resource group");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                ResourceGroupCreationDialog dialog = new ResourceGroupCreationDialog(ResourceGroupComboBox.this.getShell(), subscription);
                if (dialog.open() == Window.OK) {
                    DraftResourceGroup resourceGroupDraft = dialog.getData();
                    draftItems.add(0, resourceGroupDraft);
                    setValue(resourceGroupDraft);
                    refreshItems();
                }

            }
        });
        return button;
    }
}
