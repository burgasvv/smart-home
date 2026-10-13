package org.burgas.service.contract

import org.burgas.dao.Dao
import org.burgas.dao.File

interface ReadService<in ID, out E> where E : Dao, E : File {

    suspend fun readEntity(id: ID): E
}