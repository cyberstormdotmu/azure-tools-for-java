/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.component;

import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.util.Objects;

public class AzureTextInput extends Text implements AzureFormInputControl<String>, ModifyListener {
    protected static final int DEBOUNCE_DELAY = 300;
    private final Debouncer debouncer;
    private String value;
    
    private int setValueCount = 0;
    private boolean isUserInput = false;

    public AzureTextInput(Composite parent, Text comp, int style) {
        super(parent, style);
        this.trackValidation();
        this.debouncer = new TailingDebouncer(() -> this.fireValueChangedEvent(this.getValue()), DEBOUNCE_DELAY);
        Text text = Objects.isNull(comp) ? this.getInputControl() : comp;
        text.addModifyListener(this);
    }

    public AzureTextInput(Composite parent, int style) {
        this(parent, null, style);
    }

    public AzureTextInput(Composite parent) {
        this(parent, null, SWT.BORDER);
    }

    @Override
    public String getValue() {
        return this.value;
    }
    
    @Override
    public synchronized void setValue(String value) {
        this.setValue(value, false);
    }

    public synchronized void setValue(String value, boolean isUserInput) {
        if (this.isDisposed()) {
            return;
        }
        if (isUserInput || !this.isUserInput) {
            if (!isUserInput) {
                setValueCount++;
            }
            this.isUserInput = isUserInput;
            AzureFormInputControl.super.setValue(value);
            this.setText(value);
        }
    }

    @Override
    public synchronized void modifyText(ModifyEvent modifyEvent) {
        if (!this.isUserInput && --this.setValueCount < 0) {
            this.isUserInput = true;
        }
        this.value = this.getText();
        this.debouncer.debounce();
    }

    @Override
    public Text getInputControl() {
        return this;
    }
    
    public boolean isUserInput() {
        return this.isUserInput;
    }

    @Override
    protected void checkSubclass() {
    }
}
