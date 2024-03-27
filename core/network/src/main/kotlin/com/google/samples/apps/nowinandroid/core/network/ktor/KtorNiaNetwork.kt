/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.network.ktor

import androidx.tracing.trace
import com.google.samples.apps.nowinandroid.core.network.BuildConfig
import com.google.samples.apps.nowinandroid.core.network.NiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkChangeList
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsResource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkTopic
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ktor API declaration for NIA Network API
 */
private class NiaNetworkApi(private val client: HttpClient, private val baseUrl: String) {
    suspend fun getTopics(ids: List<String>?): NetworkResponse<List<NetworkTopic>> {
        return client.get("$baseUrl/topics") {
            ids?.let { parameter("id", ids) }
        }.body()
    }

    suspend fun getNewsResources(ids: List<String>?): NetworkResponse<List<NetworkNewsResource>> {
        return client.get("$baseUrl/newsresources") {
            ids?.let { parameter("id", ids) }
        }.body()
    }

    suspend fun getTopicChangeList(
        after: Int?,
    ): List<NetworkChangeList> {
        return client.get("$baseUrl/changelists/topics") {
            after?.let { parameter("after", after) }
        }.body()
    }

    suspend fun getNewsResourcesChangeList(after: Int?): List<NetworkChangeList> {
        return client.get("$baseUrl/changelists/newsresources") {
            after?.let { parameter("after", after) }
        }.body()
    }
}

private const val NIA_BASE_URL = BuildConfig.BACKEND_URL

/**
 * Wrapper for data provided from the [NIA_BASE_URL]
 */
@Serializable
private data class NetworkResponse<T>(
    val data: T,
)

/**
 * [Ktor] backed [NiaNetworkDataSource]
 */
@Singleton
internal class KtorNiaNetwork @Inject constructor(
    networkJson: Json,
) : NiaNetworkDataSource {

    private val networkApi = trace("KtorNiaNetwork") {
        val httpClient = HttpClient(CIO) {
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.ALL
                }
            }
            install(ContentNegotiation) {
                json(networkJson)
            }
        }
        NiaNetworkApi(httpClient, NIA_BASE_URL)
    }

    override suspend fun getTopics(ids: List<String>?): List<NetworkTopic> =
        networkApi.getTopics(ids = ids).data

    override suspend fun getNewsResources(ids: List<String>?): List<NetworkNewsResource> =
        networkApi.getNewsResources(ids = ids).data

    override suspend fun getTopicChangeList(after: Int?): List<NetworkChangeList> =
        networkApi.getTopicChangeList(after = after)

    override suspend fun getNewsResourceChangeList(after: Int?): List<NetworkChangeList> =
        networkApi.getNewsResourcesChangeList(after = after)
}
