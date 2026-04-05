package com.coloncmd.hotpot.storage.schema

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object ResponsesTable : Table("hotpot_responses") {
    val requestId = varchar("request_id", 36).references(RequestsTable.id)
    val status = integer("status")
    val body = text("body")
    val sentAt = timestamp("sent_at")
    override val primaryKey = PrimaryKey(requestId)
}
