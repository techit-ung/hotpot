package com.coloncmd.hotpot.standalone

import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.server.HotPotServer
import com.coloncmd.hotpot.standalone.config.ConfigBridge
import com.coloncmd.hotpot.standalone.config.ConfigLoader
import com.coloncmd.hotpot.storage.SqlStorage
import java.nio.file.Path

fun main() {
    val configPath = System.getenv("HOTPOT_CONFIG") ?: "hotpot.yaml"
    val config = ConfigLoader.load(Path.of(configPath))
    val storage = SqlStorage.inMemory()
    val scope = ConfigBridge.toStartScope(config, storage, NotificationService.create())
    HotPotServer(scope, config.port).start(wait = true)
}
