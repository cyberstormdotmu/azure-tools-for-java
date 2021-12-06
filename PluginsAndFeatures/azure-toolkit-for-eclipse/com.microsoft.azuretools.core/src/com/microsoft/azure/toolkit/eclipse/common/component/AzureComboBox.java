/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.azure.toolkit.ide.common.model.Draft;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;

import lombok.Getter;
import lombok.Setter;

public class AzureComboBox<T> extends Composite implements AzureFormInputControl<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final int DEBOUNCE_DELAY = 300;
    protected Object value;
    private boolean valueNotSet = true;
    protected boolean enabled = true;
    @Getter
    @Setter
    private Supplier<? extends List<? extends T>> itemsLoader;
    private AzureComboBoxViewer<T> viewer;
    private Control extension;
    private final TailingDebouncer valueDebouncer;

    public AzureComboBox(Composite parent) {
        this(parent, null, true);
    }

    public AzureComboBox(Composite parent, boolean refresh) {
        this(parent, null, refresh);
    }

    public AzureComboBox(Composite parent, @Nonnull Supplier<? extends List<? extends T>> itemsLoader) {
        this(parent, itemsLoader, true);
    }

    public AzureComboBox(Composite parent, @Nullable Supplier<? extends List<? extends T>> itemsLoader, boolean refresh) {
        super(parent, SWT.NONE);
        this.itemsLoader = itemsLoader;
        this.valueDebouncer = new TailingDebouncer(() -> AzureTaskManager.getInstance().runLater(() -> this.fireValueChangedEvent(this.getValue())), DEBOUNCE_DELAY);
        this.setupUI();
        this.init();
        if (refresh) {
            this.refreshItems();
        }
    }

    private void init() {
        this.viewer.addPostSelectionChangedListener((e) -> {
            if (!e.getSelection().isEmpty()) {
                this.setValue(this.getValue());
            }
            this.valueDebouncer.debounce();
        });
        this.trackValidation();
    }

    protected void setupUI() {
        this.viewer = new AzureComboBoxViewer<>(this);
        this.extension = this.getExtension();
        final int columns = Objects.nonNull(extension) ? 2 : 1;
        final GridLayout gridLayout = new GridLayout(columns, false);
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        this.setLayout(gridLayout);
        Optional.ofNullable(extension).ifPresent(e -> {
            e.setParent(this);
            final GridData grid = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
            grid.widthHint = 92;
            grid.minimumWidth = 92;
            e.setLayoutData(grid);
            e.setSize(92, e.getSize().y);
        });
        this.toggleLoadingSpinner(false);
        this.viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        this.viewer.setEditable(true);
        this.viewer.setContentProvider(ArrayContentProvider.getInstance());
        this.viewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return AzureComboBox.this.getItemIcon(element);
            }

            @Override
            public String getText(Object element) {
                return AzureComboBox.this.getItemText(element);
            }
        });
    }

    @Override
    public T getValue() {
        return AzureTaskManager.getInstance()
            .runAndWaitAsObservable(new AzureTask<>(() -> (T) this.viewer.getStructuredSelection().getFirstElement()))
            .toBlocking().first();
    }

    @Override
    public void setValue(final T val) {
        this.setValue(val, null);
    }

    public void setValue(final T val, final Boolean fixed) {
        Optional.ofNullable(fixed).ifPresent(f -> {
            this.setEnabled(!f);
            this.setEditable(!f);
        });
        this.valueNotSet = false;
        this.value = val;
        this.refreshValue();
    }

    public void setValue(final AzureComboBox.ItemReference<T> val) {
        this.setValue(val, null);
    }

    public void setValue(final AzureComboBox.ItemReference<T> val, final Boolean fixed) {
        Optional.ofNullable(fixed).ifPresent(f -> {
            this.setEnabled(!f);
            this.setEditable(!f);
        });
        this.valueNotSet = false;
        this.value = val;
        this.refreshValue();
    }

    private void refreshValue() {
        if (this.isDisposed()) {
            return;
        }
        if (this.valueNotSet) {
            if (this.viewer.getItemCount() > 0 && this.viewer.getSelectedIndex() != 0) {
                this.viewer.setSelectedItem(this.viewer.getItems().get(0));
            }
        } else {
            final Object selected = this.viewer.getSelectedItem();
            if (Objects.equals(selected, this.value) || (this.value instanceof AzureComboBox.ItemReference && ((AzureComboBox.ItemReference<?>) this.value).is(selected))) {
                return;
            }
            final List<T> items = this.getItems();
            if (this.value instanceof AzureComboBox.ItemReference) {
                items.stream().filter(i -> ((AzureComboBox.ItemReference<?>) this.value).is(i)).findFirst().ifPresent(this::setValue);
            } else if (items.contains(this.value)) {
                this.viewer.setSelectedItem(this.value);
            } else if (this.value instanceof Draft) {
                // todo: unify model for custom created resource
                this.viewer.add((T) this.value);
                this.viewer.setSelectedItem(this.value);
            } else {
                this.viewer.setSelectedItem(null);
            }
            this.valueDebouncer.debounce();
        }
    }

    public void refreshItems() {
        final String title = String.format("load/refresh %s items in combo box", this.getLabel());
        AzureTaskManager.getInstance().runInBackground(title, this::doRefreshItems);
    }

    @AzureOperation(
        name = "common|combobox.load_items",
        params = {"this.getLabel()"},
        type = AzureOperation.Type.ACTION
    )
    private void doRefreshItems() {
        this.setLoading(true);
        this.setItems(this.loadItemsInner());
        this.setLoading(false);
    }

    protected final List<? extends T> loadItemsInner() {
        try {
            if (Objects.nonNull(this.itemsLoader)) {
                return this.itemsLoader.get();
            } else {
                return this.loadItems();
            }
        } catch (final Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (!(rootCause instanceof InterruptedIOException) && !(rootCause instanceof InterruptedException)) {
                return Collections.emptyList();
            }
            this.handleLoadingError(e);
            return Collections.emptyList();
        }
    }

    @Nonnull
    protected List<? extends T> loadItems() throws Exception {
        return Collections.emptyList();
    }

    public List<T> getItems() {
        return this.viewer.getItems();
    }

    protected synchronized void setItems(final List<? extends T> items) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (this.isDisposed()) {
                return;
            }
            this.viewer.setItems(items);
            this.refreshValue();
        });
    }

    public void clear() {
        this.value = null;
        this.valueNotSet = true;
        this.viewer.removeAllItems();
        this.refreshValue();
    }

    protected void setLoading(final boolean loading) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (this.isDisposed()) {
                return;
            }
            if (loading) {
                this.viewer.setEnabled(false);
                this.toggleLoadingSpinner(true);
            } else {
                this.viewer.setEnabled(this.enabled);
                this.toggleLoadingSpinner(false);
            }
            this.viewer.repaint();
        });
    }

    private void toggleLoadingSpinner(boolean b) {

    }

    private void setEditable(boolean b) {
        this.viewer.setEditable(b);
    }

    public void setEnabled(boolean b) {
        this.enabled = b;
        Optional.ofNullable(this.extension).ifPresent(e -> e.setEnabled(b));
        this.viewer.setEnabled(b);
    }

    public boolean isEnabled() {
        return this.enabled || this.viewer.isEnabled();
    }

    protected String getItemText(Object item) {
        if (item == null) {
            return StringUtils.EMPTY;
        }
        return item.toString();
    }

    @Nullable
    protected Image getItemIcon(Object item) {
        return null;
    }

    @Nullable
    protected Control getExtension() {
        return null;
    }

    protected void handleLoadingError(Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureMessager.getMessager().error(e);
    }

    @Override
    public Control getInputControl() {
        return this.viewer.getControl();
    }

    public static class ItemReference<T> {
        private final Predicate<? super T> predicate;

        public ItemReference(@Nonnull Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        public ItemReference(@Nonnull Object val, Function<T, ?> mapper) {
            this.predicate = t -> Objects.equals(val, mapper.apply(t));
        }

        public boolean is(Object obj) {
            if (Objects.isNull(obj)) {
                return false;
            }
            return this.predicate.test((T) obj);
        }
    }

    public Supplier<? extends List<? extends T>> getItemsLoader() {
        return itemsLoader;
    }

    public void setItemsLoader(Supplier<? extends List<? extends T>> itemsLoader) {
        this.itemsLoader = itemsLoader;
    }
}
