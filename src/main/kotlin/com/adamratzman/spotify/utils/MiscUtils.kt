/* Spotify Web API - Kotlin Wrapper; MIT License, 2019; Original author: Adam Ratzman */
package com.adamratzman.spotify.utils

import com.adamratzman.spotify.models.BadRequestException
import com.adamratzman.spotify.models.ResultEnum

internal fun jsonMap(vararg pairs: Pair<String, Any>) = pairs.toMap().toMutableMap()

internal fun <T> catch(function: () -> T): T? {
    return try {
        function()
    } catch (e: BadRequestException) {
        if (e.statusCode != 400) throw e // we should only ignore the exception if it's a bad request (400)
        null
    }
}

internal fun <T : ResultEnum> Array<T>.match(identifier: String) =
        firstOrNull { it.retrieveIdentifier().toString().equals(identifier, true) }