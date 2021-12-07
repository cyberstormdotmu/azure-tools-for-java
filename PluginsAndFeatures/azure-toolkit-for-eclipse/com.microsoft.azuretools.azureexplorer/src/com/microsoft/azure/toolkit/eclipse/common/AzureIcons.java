/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common;

import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.program.Program;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azuretools.azureexplorer.Activator;

public class AzureIcons {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";
    public static final String FOLDER_ICON_PATH = "/icons/storagefolder.png";

    public static ImageDescriptor getIcon(String input) {
        if (StringUtils.startsWith(input, FILE_EXTENSION_ICON_PREFIX)) {
            final String fileExtension = StringUtils.removeStart(input, FILE_EXTENSION_ICON_PREFIX);
            if (StringUtils.equalsAnyIgnoreCase("root", "folder")) {
                return Activator.getImageDescriptor(FOLDER_ICON_PATH);
            }
            final Program program = org.eclipse.swt.program.Program.findProgram(fileExtension);
            return Optional.ofNullable(program).map(Program::getImageData).map(ImageDescriptor::createFromImageData).orElse(null);
        }
        final String extension = FilenameUtils.getExtension(input);
        final String iconPath = StringUtils.equals(extension, "svg")
                ? FilenameUtils.removeExtension(input) + FilenameUtils.EXTENSION_SEPARATOR + "png"
                : input;
        return Activator.getImageDescriptor(iconPath);
    }
}
