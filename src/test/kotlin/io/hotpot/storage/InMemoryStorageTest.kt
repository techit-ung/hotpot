package io.hotpot.storage

class InMemoryStorageTest : StorageContractTest() {
    override fun storage() = InMemoryStorage()
}
