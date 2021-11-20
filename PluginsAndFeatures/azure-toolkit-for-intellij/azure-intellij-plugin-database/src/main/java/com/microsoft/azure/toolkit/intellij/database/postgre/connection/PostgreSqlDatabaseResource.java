/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.postgre.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.database.Database;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlDatabase;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils.ACCESS_DENIED_ERROR_CODE;

@Setter
@Getter
public class PostgreSqlDatabaseResource extends AzureServiceResource<PostgreSqlDatabase> {

    @Nonnull
    private final Database database;

    @Override
    public String getName() {
        return database.getServerName() + "/" + database.getName();
    }


    public PostgreSqlDatabaseResource(PostgreSqlDatabase database, @Nonnull String username, @Nonnull Definition<PostgreSqlDatabase> definition) {
        super(database, definition);
        this.database = new Database(ResourceId.fromString(database.id()).parent().id(), database.getName());
        this.database.setUsername(username);
    }

    public PostgreSqlDatabaseResource(String id, @Nonnull String username, @Nonnull Definition<PostgreSqlDatabase> definition) {
        super(id, definition);
        ResourceId resourceId = ResourceId.fromString(id);
        this.database = new Database(resourceId.parent().id(), resourceId.name());
        this.database.setUsername(username);
    }

    @Override
    public void navigate(AnActionEvent event) {
        final ResourceId parent = ResourceId.fromString(this.getDataId()).parent();
        final PostgreSqlServer postgreSqlServer = Azure.az(AzurePostgreSql.class).get(parent.subscriptionId(), parent.resourceGroupName(), parent.name());
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SHOW_PROPERTIES).handle(postgreSqlServer, event);
    }

    public String loadPassword() {
        Password password = getPassword();
        if (Objects.nonNull(password) && password.saveType() == Password.SaveType.NEVER) {
            return null;
        }
        final String defName = PostgreSqlResourceDefinition.INSTANCE.getName();
        if (password.saveType() == Password.SaveType.FOREVER) {
            PasswordStore.migratePassword(this.getDataId(), this.getUsername(),
                    defName, this.getDataId(), this.getUsername());
        }
        final String saved = PasswordStore.loadPassword(defName, this.getDataId(), this.getUsername(), password.saveType());
        if (password.saveType() == Password.SaveType.UNTIL_RESTART && StringUtils.isBlank(saved)) {
            return null;
        }
        final DatabaseConnectionUtils.ConnectResult result = DatabaseConnectionUtils.connectWithPing(this.getJdbcUrl(), this.getUsername(), saved);
        if (StringUtils.isNotBlank(saved) && result.isConnected()) {
            return saved;
        }
        if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
            AzureMessager.getMessager().warning(result.getMessage(), "Azure Resource Connector");
        }
        return null;
    }

    public String inputPassword(@Nonnull final Project project) {
        final AtomicReference<Password> passwordRef = new AtomicReference<>();
        AzureTaskManager.getInstance().runAndWait(AzureOperationBundle.title("postgre.update_password"), () -> {
            final PasswordDialog dialog = new PasswordDialog(project, this.database);
            if (dialog.showAndGet()) {
                final Password password = dialog.getValue();
                this.database.getPassword().saveType(password.saveType());
                PasswordStore.savePassword(PostgreSqlResourceDefinition.INSTANCE.getName(),
                        this.getDataId(), this.database.getUsername(), password.password(), password.saveType());
                passwordRef.set(password);
            }
        });
        return Optional.ofNullable(passwordRef.get()).map(c -> String.valueOf(c.password())).orElse(null);
    }

    public JdbcUrl getJdbcUrl() {
        return database.getJdbcUrl();
    }

    public String getUsername() {
        return database.getUsername();
    }

    public Password getPassword() {
        return database.getPassword();
    }

    @Nonnull
    public Database getDatabase() {
        return database;
    }

    public void setJdbcUrl(JdbcUrl url) {
        this.database.setJdbcUrl(url);
    }

    public void setPassword(Password password) {
        this.database.setPassword(password);
    }

    public void setUsername(String username) {
        this.database.setUsername(username);
    }
}
