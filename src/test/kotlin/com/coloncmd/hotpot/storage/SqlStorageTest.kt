package com.coloncmd.hotpot.storage

class SqlStorageTest : StorageContractTest() {
    override fun storage() = SqlStorage.inMemory()
}
