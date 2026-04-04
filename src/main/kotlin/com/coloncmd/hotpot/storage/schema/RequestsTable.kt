package com.coloncmd.hotpot.storage.schema

import org.jetbrains.exposed.sql.Table

object RequestsTable : Table("hotpot_requests") {
    val id         = varchar("id", 36)
    val path       = varchar("path", 1024)
    val method     = varchar("method", 10)
    val headers    = text("headers")
    val body       = text("body")
    val receivedAt = long("received_at")
    override val primaryKey = PrimaryKey(id)
}
