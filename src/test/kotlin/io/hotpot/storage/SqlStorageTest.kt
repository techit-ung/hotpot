package io.hotpot.storage

class SqlStorageTest : StorageContractTest() {
    override fun storage() = SqlStorage.inMemory()
}
