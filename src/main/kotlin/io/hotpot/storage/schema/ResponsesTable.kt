package io.hotpot.storage.schema

import org.jetbrains.exposed.sql.Table

object ResponsesTable : Table("hotpot_responses") {
    val requestId = varchar("request_id", 36).references(RequestsTable.id)
    val status    = integer("status")
    val body      = text("body")
    val sentAt    = long("sent_at")
    override val primaryKey = PrimaryKey(requestId)
}
