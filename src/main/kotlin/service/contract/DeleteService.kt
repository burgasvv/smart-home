package org.burgas.service.contract

interface DeleteService<ID> {

    suspend fun delete(id: ID)
}