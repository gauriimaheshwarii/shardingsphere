/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.backend.handler.distsql.ral;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.distsql.handler.ral.query.QueryableRALExecutor;
import org.apache.shardingsphere.distsql.handler.update.GlobalRuleRALUpdater;
import org.apache.shardingsphere.distsql.parser.statement.ral.HintRALStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.RALStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.UpdatableGlobalRuleRALStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.scaling.UpdatableScalingRALStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.ImportDatabaseConfigurationStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.LabelComputeNodeStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.RefreshDatabaseMetaDataStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.RefreshTableMetaDataStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.SetDistVariableStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.SetInstanceStatusStatement;
import org.apache.shardingsphere.distsql.parser.statement.ral.updatable.UnlabelComputeNodeStatement;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.util.exception.external.sql.type.generic.UnsupportedSQLOperationException;
import org.apache.shardingsphere.infra.util.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.handler.ProxyBackendHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.hint.HintRALBackendHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.migration.update.UpdatableScalingRALBackendHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.AlterReadwriteSplittingStorageUnitStatusStatementHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.ImportDatabaseConfigurationHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.LabelComputeNodeHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.RefreshDatabaseMetaDataHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.RefreshTableMetaDataHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.SetDistVariableHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.SetInstanceStatusHandler;
import org.apache.shardingsphere.proxy.backend.handler.distsql.ral.updatable.UnlabelComputeNodeHandler;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.readwritesplitting.distsql.parser.statement.status.AlterReadwriteSplittingStorageUnitStatusStatement;

import java.util.HashMap;
import java.util.Map;

/**
 * RAL backend handler factory.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RALBackendHandlerFactory {
    
    private static final Map<Class<? extends RALStatement>, Class<? extends RALBackendHandler<?>>> HANDLERS = new HashMap<>();
    
    static {
        HANDLERS.put(LabelComputeNodeStatement.class, LabelComputeNodeHandler.class);
        HANDLERS.put(UnlabelComputeNodeStatement.class, UnlabelComputeNodeHandler.class);
        HANDLERS.put(SetInstanceStatusStatement.class, SetInstanceStatusHandler.class);
        HANDLERS.put(SetDistVariableStatement.class, SetDistVariableHandler.class);
        HANDLERS.put(AlterReadwriteSplittingStorageUnitStatusStatement.class, AlterReadwriteSplittingStorageUnitStatusStatementHandler.class);
        HANDLERS.put(RefreshDatabaseMetaDataStatement.class, RefreshDatabaseMetaDataHandler.class);
        HANDLERS.put(RefreshTableMetaDataStatement.class, RefreshTableMetaDataHandler.class);
        HANDLERS.put(ImportDatabaseConfigurationStatement.class, ImportDatabaseConfigurationHandler.class);
    }
    
    /**
     * Create new instance of RAL backend handler.
     *
     * @param sqlStatement RAL statement
     * @param connectionSession connection session
     * @return created instance
     */
    public static ProxyBackendHandler newInstance(final RALStatement sqlStatement, final ConnectionSession connectionSession) {
        if (TypedSPILoader.contains(QueryableRALExecutor.class, sqlStatement.getClass().getName())) {
            return new QueryableRALBackendHandler<>(sqlStatement, connectionSession);
        }
        if (sqlStatement instanceof HintRALStatement) {
            return new HintRALBackendHandler((HintRALStatement) sqlStatement, connectionSession);
        }
        if (sqlStatement instanceof UpdatableScalingRALStatement) {
            return new UpdatableScalingRALBackendHandler((UpdatableScalingRALStatement) sqlStatement, connectionSession);
        }
        if (sqlStatement instanceof UpdatableGlobalRuleRALStatement) {
            return new UpdatableGlobalRuleRALBackendHandler(sqlStatement, TypedSPILoader.getService(GlobalRuleRALUpdater.class, sqlStatement.getClass().getName()));
        }
        return createRALBackendHandler(sqlStatement, connectionSession);
    }
    
    private static RALBackendHandler<?> newInstance(final Class<? extends RALBackendHandler<?>> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new UnsupportedSQLOperationException(String.format("Can not find public constructor for class `%s`", clazz.getName()));
        }
    }
    
    private static RALBackendHandler<?> createRALBackendHandler(final RALStatement sqlStatement, final ConnectionSession connectionSession) {
        Class<? extends RALBackendHandler<?>> clazz = HANDLERS.get(sqlStatement.getClass());
        ShardingSpherePreconditions.checkState(null != clazz, () -> new UnsupportedSQLOperationException(String.format("Unsupported SQL statement : %s", sqlStatement.getClass().getName())));
        RALBackendHandler<?> result = newInstance(clazz);
        result.init(sqlStatement, connectionSession);
        return result;
    }
}
