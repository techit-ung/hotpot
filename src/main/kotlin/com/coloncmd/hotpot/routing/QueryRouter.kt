package com.coloncmd.hotpot.routing

import com.coloncmd.hotpot.storage.Storage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object QueryRouter {
    fun install(app: Application, storage: Storage) = app.routing {
        route("/hotpot") {
            get("/requests") {
                val path = call.parameters["path"]
                val method = call.parameters["method"]
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                call.respond(storage.findRequests(path = path, method = method, limit = limit))
            }
            get("/requests/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val req = storage.findRequest(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(req)
            }
            get("/requests/{id}/response") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val resp = storage.findResponseFor(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(resp)
            }
            delete("/requests") {
                storage.clear()
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
