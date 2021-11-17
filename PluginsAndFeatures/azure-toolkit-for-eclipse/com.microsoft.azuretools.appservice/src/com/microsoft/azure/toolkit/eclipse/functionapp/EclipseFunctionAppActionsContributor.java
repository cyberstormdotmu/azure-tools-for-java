/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import com.microsoft.azure.toolkit.eclipse.functionapp.creation.CreateFunctionAppHandler;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.AzureFunction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class EclipseFunctionAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = FunctionAppActionsContributor.INITIALIZE_ORDER + 1;

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, Object> createCondition = (r, e) -> r instanceof AzureFunction;
        final BiConsumer<Object, Object> createHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> CreateFunctionAppHandler.createFunctionApp());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, createCondition, createHandler);
    }

    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
