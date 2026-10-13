package org.burgas.redis

import org.burgas.dto.Response

interface RedisCacheHandler<in R : Response> {

    suspend fun handleCache(response: R)
}