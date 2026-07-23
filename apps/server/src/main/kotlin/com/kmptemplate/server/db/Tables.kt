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

/**
 * Append-only player reports (trust and safety). One row per report; reads
 * are deferred to a future moderation-review surface. See
 * `V3__player_reports.sql`.
 */
object PlayerReportsTable : Table("player_reports") {
    val id = long("id").autoIncrement()
    val reporterUserId = uuid("reporter_user_id")
    val reportedUserId = uuid("reported_user_id")
    val context = text("context").nullable()
    val reason = text("reason").nullable()
    val reasonCategories = text("reason_categories").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
