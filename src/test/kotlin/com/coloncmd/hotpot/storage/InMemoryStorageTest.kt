package com.coloncmd.hotpot.storage

class InMemoryStorageTest : StorageContractTest() {
    override fun storage() = InMemoryStorage()
}
