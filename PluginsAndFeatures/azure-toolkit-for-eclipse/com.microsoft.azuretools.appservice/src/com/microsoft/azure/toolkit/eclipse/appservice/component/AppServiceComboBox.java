package com.microsoft.azure.toolkit.eclipse.appservice.component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;

public abstract class AppServiceComboBox<T extends AppServiceConfig> extends AzureComboBox<T> {

    protected T configModel;

    public AppServiceComboBox(Composite parent) {
        super(parent, false);
    }

    public T getConfigModel() {
        return configModel;
    }

    public void setConfigModel(T configModel) {
        this.configModel = configModel;
    }

    @Nonnull
    @Override
    protected List<? extends T> loadItems() throws Exception {
        final List<T> items = loadAppServiceModels();
        if (isDraftResource(configModel)) {
            final boolean exist = items.stream().anyMatch(item -> AppServiceConfig.isSameApp(item, configModel));
            if (!exist) {
                items.add(configModel);
            }
        }
        return items;
    }

    @Override
    public T getValue() {
        if (value instanceof ItemReference && ((ItemReference<?>) value).is(configModel)) {
            return configModel;
        }
        return super.getValue();
    }
    
    @Override
    protected String getItemText(final Object item) {
        if (item instanceof AppServiceConfig) {
            final AppServiceConfig selectedItem = (AppServiceConfig) item;
            return isDraftResource(selectedItem) ? String.format("(New) %s", selectedItem.getName()) : selectedItem.getName();
        } else {
            return super.getItemText(item);
        }
    }

    @Override
    protected Control getExtension() {
        final Button button = new Button(this, SWT.PUSH);
        button.setText("Create");
        button.setToolTipText("Create New App");
        button.addListener(SWT.Selection, event -> this.createResource());
        return button;
    }

    // todo: move to toolkit lib
    public static boolean isJavaAppService(IAppService<?> appService) {
        try {
            return Optional.ofNullable(appService.getRuntime()).map(Runtime::getJavaVersion)
                    .map(javaVersion -> !Objects.equals(javaVersion, JavaVersion.OFF))
                    .orElse(false);
        } catch (final RuntimeException e) {
            // app service may have been removed while parsing, return false in this case
            return false;
        }
    }
    
    protected abstract void createResource();

    protected abstract List<T> loadAppServiceModels() throws Exception;

    private static boolean isDraftResource(final AppServiceConfig config) {
        return config != null && StringUtils.isEmpty(config.getResourceId());
    }
}
