package io.hotpot

import io.hotpot.dsl.StartScope
import io.hotpot.notification.NotificationService
import io.hotpot.server.HotPotServer
import io.hotpot.storage.InMemoryStorage
import io.hotpot.storage.SqlStorage
import io.hotpot.storage.Storage

fun start(
    port: Int = 8080,
    storage: Storage = SqlStorage.inMemory(),
    notificationService: NotificationService = NotificationService.create(),
    block: StartScope.() -> Unit,
) {
    val scope = StartScope(storage, notificationService).apply(block)
    HotPotServer(scope, port).start(wait = true)
}

fun main() {
    start {
        // Define your mock backend here using the DSL
        // listen(path = "/orders") { ... }
    }
}
