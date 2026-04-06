package com.coloncmd.hotpot

import com.coloncmd.hotpot.dsl.StartScope
import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.storage.InMemoryStorage
import com.coloncmd.hotpot.storage.SqlStorage
import com.coloncmd.hotpot.storage.Storage

fun buildScope(
    storage: Storage = SqlStorage.inMemory(),
    notificationService: NotificationService = NotificationService.create(),
    block: StartScope.() -> Unit,
): StartScope = StartScope(storage, notificationService).apply(block)

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
