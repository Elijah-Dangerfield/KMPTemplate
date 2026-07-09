package com.kmptemplate.server.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Exposed table definitions.
 *
 * Flyway SQL (resources/db/migration) is the source of truth for the schema;
 * these objects are read-side projections used to type-check queries and map
 * rows. Keep them in sync — `DatabaseSchemaTest` fails if a column declared here
 * doesn't exist in the migrated schema.
 *
 * Convention: object named `XxxTable`, SQL table name in the `Table("…")`
 * constructor, camelCase Kotlin vals mapping to snake_case columns.
 */
object ProfilesTable : Table("profiles") {
    val userId = uuid("user_id")
    val displayName = text("display_name").uniqueIndex("profiles_display_name_uq")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(userId)
}
