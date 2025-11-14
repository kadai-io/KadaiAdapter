/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.adapter.camunda.schemacreator;

import io.kadai.adapter.camunda.exceptions.UnsupportedDatabaseException;

/** Supported versions of databases. */
public enum DB {
  H2("H2", "h2"),
  DB2("DB2", "db2"),
  POSTGRESS("PostgreSQL", "postgres"),
  ORACLE("Oracle", "oracle");

  public final String dbProductname;
  public final String dbProductId;

  DB(String dbProductname, String dbProductId) {
    this.dbProductname = dbProductname;
    this.dbProductId = dbProductId;
  }

  public static boolean isDb2(String dbProductName) {
    return dbProductName != null && dbProductName.contains(DB2.dbProductname);
  }

  public static boolean isH2(String dbProductName) {
    return dbProductName != null && dbProductName.contains(H2.dbProductname);
  }

  public static boolean isPostgreSql(String dbProductName) {
    return POSTGRESS.dbProductname.equals(dbProductName);
  }

  public static boolean isOracle(String dbProductName) {
    return dbProductName != null && dbProductName.contains(ORACLE.dbProductname);
  }

  public static String getDatabaseProductId(String dbProductName) {

    if (isDb2(dbProductName)) {
      return DB2.dbProductId;
    } else if (isH2(dbProductName)) {
      return H2.dbProductId;
    } else if (isPostgreSql(dbProductName)) {
      return POSTGRESS.dbProductId;
    } else if (isOracle(dbProductName)) {
      return ORACLE.dbProductId;
    } else {
      throw new UnsupportedDatabaseException(dbProductName);
    }
  }
}
