package com.coloncmd.hotpot.storage.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ResponsesTable : Table("hotpot_responses") {
    val requestId = varchar("request_id", 36).references(RequestsTable.id)
    val status    = integer("status")
    val body      = text("body")
    val sentAt    = timestamp("sent_at")
    override val primaryKey = PrimaryKey(requestId)
}
