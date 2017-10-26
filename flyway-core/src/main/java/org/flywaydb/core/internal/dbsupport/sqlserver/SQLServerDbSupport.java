/*
 * Copyright 2010-2017 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.dbsupport.sqlserver;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.FlywayDbUpgradeRequiredException;
import org.flywaydb.core.internal.dbsupport.JdbcTemplate;
import org.flywaydb.core.internal.dbsupport.Schema;
import org.flywaydb.core.internal.dbsupport.SqlStatementBuilder;
import org.flywaydb.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * SQLServer-specific support.
 */
public class SQLServerDbSupport extends DbSupport {
    private static final Log LOG = LogFactory.getLog(SQLServerDbSupport.class);

    /**
     * Whether the warning message has already been printed.
     */
    private static boolean schemaMessagePrinted;

    /**
     * Creates a new instance.
     *
     * @param connection The connection to use.
     */
    public SQLServerDbSupport(Connection connection) {
        super(new JdbcTemplate(connection, Types.VARCHAR));
    }

    @Override
    protected final void ensureSupported() {
        int majorVersion = getMajorVersion();
        int minorVersion = getMinorVersion();
        String release = versionToReleaseName(majorVersion, minorVersion);

        if (majorVersion < 10) {
            throw new FlywayDbUpgradeRequiredException("SQL Server", release, "2008");
        }
        if (majorVersion < 12) {
            // [oss-enabled]
            //throw new org.flywaydb.core.internal.dbsupport.FlywayEnterpriseUpgradeRequiredException("Microsoft", "SQL Server", release);
            // [/oss-enabled]
        } else if (majorVersion > 14 || (majorVersion == 14 && minorVersion > 0)) {
            recommendFlywayUpgrade("SQL Server", release);
        }
    }

    private String versionToReleaseName(int major, int minor) {
        if (major < 8) {
            return major + "." + minor;
        }
        if (major == 8) {
            return "2000";
        }
        if (major == 9) {
            return "2005";
        }
        if (major == 10) {
            if (minor == 0) {
                return "2008";
            }
            return "2008 R2";
        }
        if (major == 11) {
            return "2012";
        }
        if (major == 12) {
            return "2014";
        }
        if (major == 13) {
            return "2016";
        }
        if (major == 14) {
            return "2017";
        }
        return major + "." + minor;
    }

    public String getDbName() {
        return "sqlserver";
    }

    public String getCurrentUserFunction() {
        return "SUSER_SNAME()";
    }

    @Override
    protected String doGetCurrentSchemaName() throws SQLException {
        return jdbcTemplate.queryForString("SELECT SCHEMA_NAME()");
    }

    @Override
    protected void doChangeCurrentSchemaTo(String schema) throws SQLException {
        if (!schemaMessagePrinted) {
            LOG.info("SQLServer does not support setting the schema for the current session. Default schema NOT changed to " + schema);
            // Not currently supported.
            // See http://connect.microsoft.com/SQLServer/feedback/details/390528/t-sql-statement-for-changing-default-schema-context
            schemaMessagePrinted = true;
        }
    }

    public boolean supportsDdlTransactions() {
        return true;
    }

    public String getBooleanTrue() {
        return "1";
    }

    public String getBooleanFalse() {
        return "0";
    }

    public SqlStatementBuilder createSqlStatementBuilder() {
        return new SQLServerSqlStatementBuilder();
    }

    /**
     * Escapes this identifier, so it can be safely used in sql queries.
     *
     * @param identifier The identifier to escaped.
     * @return The escaped version.
     */
    private String escapeIdentifier(String identifier) {
        return StringUtils.replaceAll(identifier, "]", "]]");
    }

    @Override
    public String doQuote(String identifier) {
        return "[" + escapeIdentifier(identifier) + "]";
    }

    @Override
    public Schema getSchema(String name) {
        return new SQLServerSchema(jdbcTemplate, this, name);
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public boolean useSingleConnection() {
        return true;
    }
}
