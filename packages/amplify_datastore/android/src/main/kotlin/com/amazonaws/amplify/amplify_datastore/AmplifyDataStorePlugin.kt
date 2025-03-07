// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazonaws.amplify.amplify_datastore

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.amazonaws.amplify.amplify_datastore.exception.ExceptionMessages
import com.amazonaws.amplify.amplify_datastore.exception.ExceptionUtil.Companion.createSerializedError
import com.amazonaws.amplify.amplify_datastore.exception.ExceptionUtil.Companion.createSerializedUnrecognizedError
import com.amazonaws.amplify.amplify_datastore.exception.ExceptionUtil.Companion.handleAddPluginException
import com.amazonaws.amplify.amplify_datastore.exception.ExceptionUtil.Companion.postExceptionToFlutterChannel
import com.amazonaws.amplify.amplify_datastore.pigeons.FlutterError
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeAmplifyBridge
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeApiBridge
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeApiPlugin
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeAuthBridge
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeAuthPlugin
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeAuthUser
import com.amazonaws.amplify.amplify_datastore.pigeons.NativeGraphQLSubscriptionResponse
import com.amazonaws.amplify.amplify_datastore.types.model.FlutterCustomTypeSchema
import com.amazonaws.amplify.amplify_datastore.types.model.FlutterModelSchema
import com.amazonaws.amplify.amplify_datastore.types.model.FlutterSerializedModel
import com.amazonaws.amplify.amplify_datastore.types.model.FlutterSubscriptionEvent
import com.amazonaws.amplify.amplify_datastore.types.query.QueryOptionsBuilder
import com.amazonaws.amplify.amplify_datastore.types.query.QueryPredicateBuilder
import com.amazonaws.amplify.amplify_datastore.util.AtomicResult
import com.amazonaws.amplify.amplify_datastore.util.cast
import com.amazonaws.amplify.amplify_datastore.util.safeCastToList
import com.amazonaws.amplify.amplify_datastore.util.safeCastToMap
import com.amplifyframework.AmplifyException
import com.amplifyframework.annotations.AmplifyFlutterApi
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.api.aws.AuthModeStrategyType
import com.amplifyframework.api.aws.AuthorizationType
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.async.Cancelable
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.core.model.CustomTypeSchema
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.SerializedCustomType
import com.amplifyframework.core.model.SerializedModel
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.AWSDataStorePlugin
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.DataStoreConflictHandler
import com.amplifyframework.datastore.DataStoreErrorHandler
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.DataStorePlugin
import com.amplifyframework.util.UserAgent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

typealias ResolutionStrategy = DataStoreConflictHandler.ResolutionStrategy

/** AmplifyDataStorePlugin */
class AmplifyDataStorePlugin :
    FlutterPlugin,
    MethodCallHandler,
    NativeAmplifyBridge,
    NativeAuthBridge,
    NativeApiBridge {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var observeCancelable: Cancelable? = null
    private lateinit var hubEventChannel: EventChannel

    private val dataStoreObserveEventStreamHandler: DataStoreObserveEventStreamHandler
    private val dataStoreHubEventStreamHandler: DataStoreHubEventStreamHandler
    private val uiThreadHandler: Handler
    private val LOG = Amplify.Logging.forNamespace("amplify:flutter:datastore")
    private var isSettingUpObserve = AtomicBoolean()
    private var nativeApiPlugin: NativeApiPlugin? = null
    private val coroutineScope = CoroutineScope(CoroutineName("AmplifyFlutterPlugin"))
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private lateinit var context: Context

    /**
     * The local cache of the current Dart user.
     */
    private var currentUser: AuthUser? = null

    private companion object {
        /**
         * API authorization providers configured during the `addPlugin` call.
         *
         * The auth providers require a reference to the active method channel to be able to
         * communicate back to Dart code. If the app is moved to the background and resumed,
         * the `Amplify.addPlugin` call does not re-configure auth providers, so these must
         * be instantiated only once but still maintain a reference to the active method channel.
         */
        var flutterAuthProviders: FlutterAuthProviders? = null
        var nativeAuthPlugin: NativeAuthPlugin? = null
        var hasAddedUserAgent :Boolean = false
    }

    val modelProvider = FlutterModelProvider.instance
    val _injectedPlugin: AWSDataStorePlugin?
    val dataStorePlugin: AWSDataStorePlugin
        get() = _injectedPlugin ?: Amplify.DataStore.getPlugin("awsDataStorePlugin") as AWSDataStorePlugin

    constructor() {
        dataStoreObserveEventStreamHandler = DataStoreObserveEventStreamHandler()
        dataStoreHubEventStreamHandler = DataStoreHubEventStreamHandler()
        _injectedPlugin = null
        uiThreadHandler = Handler(Looper.getMainLooper())
    }

    @VisibleForTesting
    constructor(
        eventHandler: DataStoreObserveEventStreamHandler,
        hubEventHandler: DataStoreHubEventStreamHandler
    ) {
        dataStoreObserveEventStreamHandler = eventHandler
        dataStoreHubEventStreamHandler = hubEventHandler
        _injectedPlugin = null
        uiThreadHandler = Handler(Looper.getMainLooper())
    }
    internal constructor(
        dataStorePlugin: AWSDataStorePlugin,
        uiThreadHandler: Handler,
        eventHandler: DataStoreObserveEventStreamHandler,
        hubEventHandler: DataStoreHubEventStreamHandler
    ) {
        dataStoreObserveEventStreamHandler = eventHandler
        dataStoreHubEventStreamHandler = hubEventHandler
        this._injectedPlugin = dataStorePlugin
        this.uiThreadHandler = uiThreadHandler
    }

    override fun onAttachedToEngine(
        flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    ) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "com.amazonaws.amplify/datastore"
        )
        channel.setMethodCallHandler(this)
        eventChannel = EventChannel(
            flutterPluginBinding.binaryMessenger,
            "com.amazonaws.amplify/datastore_observe_events"
        )
        eventChannel.setStreamHandler(dataStoreObserveEventStreamHandler)

        hubEventChannel = EventChannel(
            flutterPluginBinding.binaryMessenger,
            "com.amazonaws.amplify/datastore_hub_events"
        )
        hubEventChannel.setStreamHandler(dataStoreHubEventStreamHandler)

        nativeAuthPlugin = NativeAuthPlugin(flutterPluginBinding.binaryMessenger)
        NativeAuthBridge.setUp(flutterPluginBinding.binaryMessenger, this)

        nativeApiPlugin = NativeApiPlugin(flutterPluginBinding.binaryMessenger)
        NativeApiBridge.setUp(flutterPluginBinding.binaryMessenger, this)

        NativeAmplifyBridge.setUp(flutterPluginBinding.binaryMessenger, this)

        LOG.info("Initiated DataStore plugin")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)

        eventChannel.setStreamHandler(null)
        hubEventChannel.setStreamHandler(null)

        observeCancelable?.cancel()
        observeCancelable = null

        dataStoreHubEventStreamHandler.onCancel(null)

        nativeAuthPlugin = null
        NativeAuthBridge.setUp(binding.binaryMessenger, null)

        nativeApiPlugin = null
        NativeApiBridge.setUp(binding.binaryMessenger, null)

        NativeAmplifyBridge.setUp(binding.binaryMessenger, null)
    }

    override fun onMethodCall(call: MethodCall, _result: Result) {
        val result = AtomicResult(_result, call.method)
        var data: Map<String, Any> = HashMap()
        try {
            if (call.arguments != null) {
                data = checkArguments(call.arguments) as HashMap<String, Any>
            }
        } catch (e: Exception) {
            uiThreadHandler.post {
                postExceptionToFlutterChannel(
                    result,
                    "DataStoreException",
                    createSerializedUnrecognizedError(e),
                    uiThreadHandler
                )
            }
            return
        }
        when (call.method) {
            "query" -> onQuery(result, data)
            "delete" -> onDelete(result, data)
            "save" -> onSave(result, data)
            "clear" -> onClear(result)
            "setUpObserve" -> onSetUpObserve(result)
            "configureDataStore" -> onConfigureDataStore(result, data)
            "start" -> onStart(result)
            "stop" -> onStop(result)
            else -> result.notImplemented()
        }
    }

    @VisibleForTesting
    fun onConfigureDataStore(flutterResult: Result, request: Map<String, Any>) {
        if (!request.containsKey("modelSchemas") || !request.containsKey(
                "modelProviderVersion"
            ) || request["modelSchemas"] !is List<*>
        ) {
            uiThreadHandler.post {
                postExceptionToFlutterChannel(
                    flutterResult,
                    "DataStoreException",
                    createSerializedError(
                        ExceptionMessages.missingExceptionMessage,
                        ExceptionMessages.missingRecoverySuggestion,
                        "Received invalid request from Dart, modelSchemas and/or modelProviderVersion" +
                            " are not available. Request: " + request.toString()
                    ),
                    uiThreadHandler
                )
            }
            return
        }

        // Register schemas to the native model provider
        registerSchemas(request)

        val syncExpressions: List<Map<String, Any>> =
            request["syncExpressions"].safeCastToList() ?: emptyList()
        val defaultDataStoreConfiguration = DataStoreConfiguration.defaults()
        val authModeStrategy = request["authModeStrategy"] as String
        val authModeStrategyType = AuthModeStrategyType.valueOf(authModeStrategy.uppercase())
        val syncInterval: Long =
            (request["syncInterval"] as? Int)?.toLong()
                ?: defaultDataStoreConfiguration.syncIntervalInMinutes
        val syncMaxRecords: Int =
            (request["syncMaxRecords"] as? Int)
                ?: defaultDataStoreConfiguration.syncMaxRecords
        val syncPageSize: Int =
            (request["syncPageSize"] as? Int)
                ?: defaultDataStoreConfiguration.syncPageSize

        val dataStoreConfigurationBuilder = DataStoreConfiguration.builder()

        try {
            buildSyncExpressions(syncExpressions, dataStoreConfigurationBuilder)
        } catch (e: Exception) {
            uiThreadHandler.post {
                postExceptionToFlutterChannel(
                    flutterResult,
                    "DataStoreException",
                    createSerializedUnrecognizedError(e),
                    uiThreadHandler
                )
            }
        }

        dataStoreConfigurationBuilder.errorHandler(createErrorHandler(request))
        dataStoreConfigurationBuilder.conflictHandler(createConflictHandler(request))

        val dataStorePlugin = AWSDataStorePlugin
            .builder()
            .modelProvider(modelProvider)
            .authModeStrategy(authModeStrategyType)
            .dataStoreConfiguration(
                dataStoreConfigurationBuilder
                    .syncInterval(syncInterval, TimeUnit.MINUTES)
                    .syncMaxRecords(syncMaxRecords)
                    .syncPageSize(syncPageSize)
                    .build()
            )
            .build()

        try {
            Amplify.addPlugin(dataStorePlugin)
        } catch (e: Exception) {
            handleAddPluginException("Datastore", e, flutterResult, uiThreadHandler)
            return
        }
        flutterResult.success(null)
    }

    @VisibleForTesting
    fun onQuery(flutterResult: Result, request: Map<String, Any>) {
        val modelName: String
        val queryOptions: QueryOptions
        try {
            modelName = request["modelName"] as String
            val modelSchema = modelProvider.modelSchemas().getValue(modelName)
            queryOptions = QueryOptionsBuilder.fromSerializedMap(request, modelSchema)
        } catch (e: Exception) {
            uiThreadHandler.post {
                postExceptionToFlutterChannel(
                    flutterResult,
                    "DataStoreException",
                    createSerializedUnrecognizedError(e),
                    uiThreadHandler
                )
            }
            return
        }

        val plugin = dataStorePlugin
        plugin.query(
            modelName,
            queryOptions,
            {
                try {
                    val results: List<Map<String, Any>> =
                        it.asSequence().toList().map { model: Model? ->
                            FlutterSerializedModel(model as SerializedModel).toMap()
                        }
                    LOG.debug("Number of items received " + results.size)

                    uiThreadHandler.post { flutterResult.success(results) }
                } catch (e: Exception) {
                    uiThreadHandler.post {
                        postExceptionToFlutterChannel(
                            flutterResult,
                            "DataStoreException",
                            createSerializedUnrecognizedError(e),
                            uiThreadHandler
                        )
                    }
                }
            },
            {
                LOG.error("Query operation failed.", it)
                uiThreadHandler.post {
                    postExceptionToFlutterChannel(
                        flutterResult,
                        "DataStoreException",
                        createSerializedError(it),
                        uiThreadHandler
                    )
                }
            }
        )
    }

    @VisibleForTesting
    fun onDelete(flutterResult: Result, request: Map<String, Any>) {
        val modelName: String
        val queryPredicate: QueryPredicate
        val serializedModelData: Map<String, Any?>
        val schema: ModelSchema

        try {
            modelName = request["modelName"] as String
            schema = modelProvider.modelSchemas()[modelName]!!
            serializedModelData =
                deserializeNestedModel(request["serializedModel"].safeCastToMap()!!, schema)

            queryPredicate = QueryPredicateBuilder.fromSerializedMap(
                request["queryPredicate"].safeCastToMap()
            ) ?: QueryPredicates.all()
        } catch (e: Exception) {
            uiThreadHandler.post {
                postExceptionToFlutterChannel(
                    flutterResult,
                    "DataStoreException",
                    createSerializedUnrecognizedError(e),
                    uiThreadHandler
                )
            }
            return
        }

        val plugin = dataStorePlugin

        val instance = SerializedModel.builder()
            .modelSchema(schema)
            .serializedData(serializedModelData)
            .build()

        plugin.delete(
            instance,
            queryPredicate,
            {
                LOG.info("Deleted item: " + it.item().toString())
                uiThreadHandler.post { flutterResult.success(null) }
            },
            {
                LOG.error("Delete operation failed.", it)
                if (it.localizedMessage == "Wanted to delete one row, but deleted 0 rows.") {
                    uiThreadHandler.post { flutterResult.success(null) }
                } else {
                    uiThreadHandler.post {
                        postExceptionToFlutterChannel(
                            flutterResult,
                            "DataStoreException",
                            createSerializedError(it),
                            uiThreadHandler
                        )
                    }
                }
            }
        )
    }

    @VisibleForTesting
    fun onSave(flutterResult: Result, request: Map<String, Any>) {
        val modelName: String
        val queryPredicate: QueryPredicate
        val serializedModelData: Map<String, Any?>
        val schema: ModelSchema

        try {
            modelName = request["modelName"] as String
            schema = modelProvider.modelSchemas()[modelName]!!
            serializedModelData =
                deserializeNestedModel(request["serializedModel"].safeCastToMap()!!, schema)

            queryPredicate = QueryPredicateBuilder.fromSerializedMap(
                request["queryPredicate"].safeCastToMap()
                ) ?: QueryPredicates.all()
        } catch (e: Exception) {
            uiThreadHandler.post {
                postExceptionToFlutterChannel(
                    flutterResult,
                    "DataStoreException",
                    createSerializedUnrecognizedError(e),
                    uiThreadHandler
                )
            }
            return
        }

        val plugin = dataStorePlugin

        val serializedModel = SerializedModel.builder()
            .modelSchema(schema)
            .serializedData(serializedModelData)
            .build()

        plugin.save(
            serializedModel,
            queryPredicate,
            {
                LOG.info("Saved item: " + it.item().toString())
                uiThreadHandler.post { flutterResult.success(null) }
            },
            {
                LOG.error("Save operation failed", it)
                uiThreadHandler.post {
                    postExceptionToFlutterChannel(
                        flutterResult,
                        "DataStoreException",
                        createSerializedError(it),
                        uiThreadHandler
                    )
                }
            }
        )
    }

    fun onClear(flutterResult: Result) {
        val plugin = dataStorePlugin

        plugin.clear(
            {
                LOG.info("Successfully cleared the store")
                uiThreadHandler.post { flutterResult.success(null) }
            },
            {
                LOG.error("Failed to clear store with error: ", it)
                uiThreadHandler.post {
                    postExceptionToFlutterChannel(
                        flutterResult,
                        "DataStoreException",
                        createSerializedError(it),
                        uiThreadHandler
                    )
                }
            }
        )
    }

    fun onSetUpObserve(flutterResult: Result) {
        if (observeCancelable != null || isSettingUpObserve.getAndSet(true)) {
            flutterResult.success(true)
            return
        }

        val plugin = dataStorePlugin
        plugin.observe(
            { cancelable ->
                LOG.info("Established a new stream form flutter $cancelable")
                observeCancelable = cancelable
                isSettingUpObserve.set(false)
                flutterResult.success(true)
            },
            { event ->
                LOG.debug("Received event: $event")
                if (event.item() is SerializedModel) {
                    dataStoreObserveEventStreamHandler.sendEvent(
                        FlutterSubscriptionEvent(
                            serializedModel = event.item() as SerializedModel,
                            eventType = event.type().name.lowercase(Locale.getDefault())
                        ).toMap()
                    )
                }
            },
            { failure: DataStoreException ->
                if (failure.message?.contains("Failed to start DataStore", true) == true) {
                    isSettingUpObserve.set(false)
                    flutterResult.success(false)
                } else {
                    LOG.error("Received an error", failure)
                    dataStoreObserveEventStreamHandler.error(
                        "DataStoreException",
                        createSerializedError(failure)
                    )
                }
            },
            { LOG.info("Observation complete.") }
        )
    }

    @VisibleForTesting
    fun onStart(flutterResult: Result) {
        val plugin = dataStorePlugin

        plugin.start(
            {
                LOG.info("Successfully started datastore remote synchronization")
                uiThreadHandler.post {
                    flutterResult.success(null)
                }
            },
            {
                LOG.error("Failed to start datastore with error: ", it)
                uiThreadHandler.post {
                    postExceptionToFlutterChannel(
                        flutterResult,
                        "DataStoreException",
                        createSerializedError(it),
                        uiThreadHandler
                    )
                }
            }
        )
    }

    @VisibleForTesting
    fun onStop(flutterResult: Result) {
        val plugin = dataStorePlugin

        plugin.stop(
            {
                LOG.info("Successfully stopped datastore remote synchronization")
                uiThreadHandler.post {
                    flutterResult.success(null)
                }
            },
            {
                LOG.error("Failed to stop datastore with error: ", it)
                uiThreadHandler.post {
                    postExceptionToFlutterChannel(
                        flutterResult,
                        "DataStoreException",
                        createSerializedError(it),
                        uiThreadHandler
                    )
                }
            }
        )
    }

    private fun checkArguments(args: Any): Map<String, Any> {
        if (args !is Map<*, *>) {
            throw java.lang.Exception("Flutter method call arguments are not a map.")
        }
        return args.safeCastToMap()!!
    }

    private fun buildSyncExpressions(
        syncExpressions: List<Map<String, Any>>,
        dataStoreConfigurationBuilder: DataStoreConfiguration.Builder
    ) {
        syncExpressions.forEach {
            try {
                val id = it["id"] as String
                val modelName = it["modelName"] as String
                val queryPredicate =
                    QueryPredicateBuilder.fromSerializedMap(it["queryPredicate"].safeCastToMap())!!
                dataStoreConfigurationBuilder.syncExpression(modelName) {
                    var resolvedQueryPredicate = queryPredicate
                    val latch = CountDownLatch(1)
                    uiThreadHandler.post {
                        channel.invokeMethod(
                            "resolveQueryPredicate",
                            id,
                            object : Result {
                                override fun success(result: Any?) {
                                    try {
                                        resolvedQueryPredicate =
                                            QueryPredicateBuilder.fromSerializedMap(result.safeCastToMap())!!
                                    } catch (e: Exception) {
                                        LOG.error("Failed to resolve query predicate. Reverting to original query predicate.")
                                    }
                                    latch.countDown()
                                }

                                override fun error(code: String, msg: String?, details: Any?) {
                                    LOG.error("Failed to resolve query predicate. Reverting to original query predicate.")
                                    latch.countDown()
                                }

                                override fun notImplemented() {
                                    LOG.error("resolveQueryPredicate not implemented.")
                                    latch.countDown()
                                }
                            }
                        )
                    }
                    try {
                        latch.await()
                    } catch (e: InterruptedException) {
                        LOG.error(
                            "Failed to resolve query predicate due to $e. Reverting to original query " +
                                "predicate."
                        )
                    }
                    resolvedQueryPredicate
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    @VisibleForTesting
    fun deserializeNestedModel(serializedModelData: Map<String, Any?>, modelSchema: ModelSchema): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // iterate over schema fields to deserialize each field value
        for ((key, field) in modelSchema.fields.entries) {
            // ignore if serializedModelData doesn't contain value for the current field key
            if (!serializedModelData.containsKey(key)) {
                continue
            }
            val fieldSerializedData = serializedModelData[key]

            // if the field serialized value is null
            // assign null to this field in the deserialized map
            if (fieldSerializedData == null) {
                result[key] = fieldSerializedData
                continue
            }

            if (field.isModel) {
                // ignore field if the field doesn't have valid schema in ModelProvider
                val fieldModelSchema = modelProvider.modelSchemas()[field.targetType] ?: continue
                result[key] = SerializedModel.builder()
                    .modelSchema(fieldModelSchema)
                    .serializedData(deserializeNestedModel(fieldSerializedData as Map<String, Any?>, fieldModelSchema))
                    .build()
            } else if (field.isCustomType) {
                // ignore field if the field doesn't have valid schema in ModelProvider
                val fieldCustomTypeSchema = modelProvider.customTypeSchemas()[field.targetType] ?: continue
                val deserializedCustomType = getDeserializedCustomTypeField(
                    fieldCustomTypeSchema = fieldCustomTypeSchema,
                    isFieldArray = field.isArray,
                    listOfSerializedData = if (field.isArray) fieldSerializedData as List<Map<String, Any?>> else null,
                    serializedData = if (!field.isArray) fieldSerializedData as Map<String, Any?> else null
                )

                if (deserializedCustomType != null) {
                    result[key] = deserializedCustomType
                }
            } else {
                result[key] = fieldSerializedData
            }
        }

        return result.toMap()
    }

    private fun deserializeNestedCustomType(
        serializedModelData: Map<String, Any?>,
        customTypeSchema: CustomTypeSchema
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        for ((key, field) in customTypeSchema.fields.entries) {
            if (!serializedModelData.containsKey(key)) {
                continue
            }

            val fieldSerializedData = serializedModelData[key]

            // if the field serialized value is null
            // assign null to this field in the deserialized map
            if (fieldSerializedData == null) {
                result[key] = fieldSerializedData
                continue
            }

            if (field.isCustomType) {
                // ignore field if the field doesn't have valid schema in ModelProvider
                val fieldCustomTypeSchema = modelProvider.customTypeSchemas()[field.targetType] ?: continue
                val deserializedCustomType = getDeserializedCustomTypeField(
                    fieldCustomTypeSchema = fieldCustomTypeSchema,
                    isFieldArray = field.isArray,
                    listOfSerializedData = if (field.isArray) fieldSerializedData as List<Map<String, Any?>> else null,
                    serializedData = if (!field.isArray) fieldSerializedData as Map<String, Any?> else null
                )

                if (deserializedCustomType != null) {
                    result[key] = deserializedCustomType
                }
            } else {
                result[key] = fieldSerializedData
            }
        }

        return result.toMap()
    }

    private fun getDeserializedCustomTypeField(
        fieldCustomTypeSchema: CustomTypeSchema,
        isFieldArray: Boolean = false,
        listOfSerializedData: List<Map<String, Any?>>? = null,
        serializedData: Map<String, Any?>? = null
    ): Any? {
        // When a field is custom type
        // the field value can only be a single custom type
        // or a list of item of the same custom type
        if (isFieldArray && listOfSerializedData != null) {
            return listOfSerializedData.map {
                SerializedCustomType.builder()
                    .serializedData(deserializeNestedCustomType(it, fieldCustomTypeSchema))
                    .customTypeSchema(fieldCustomTypeSchema)
                    .build()
            }
        }

        if (serializedData != null) {
            return SerializedCustomType.builder()
                .serializedData(deserializeNestedCustomType(serializedData, fieldCustomTypeSchema))
                .customTypeSchema(fieldCustomTypeSchema)
                .build()
        }

        return null
    }

    private fun registerSchemas(request: Map<String, Any>) {
        val modelSchemas: List<Map<String, Any>> = request["modelSchemas"].safeCastToList()!!
        val customTypeSchemas: List<Map<String, Any>> = request["customTypeSchemas"].safeCastToList() ?: emptyList()
        modelProvider.setVersion(request["modelProviderVersion"] as String)

        val flutterModelSchemaList =
            modelSchemas.map { FlutterModelSchema(it) }

        flutterModelSchemaList.forEach {
            val nativeSchema = it.convertToNativeModelSchema()
            modelProvider.addModelSchema(
                it.name,
                nativeSchema
            )
        }

        if (customTypeSchemas.isNotEmpty()) {
            val flutterCustomTypeSchemaList =
                customTypeSchemas.map { FlutterCustomTypeSchema(it) }

            flutterCustomTypeSchemaList.forEach {
                val nativeSchema = it.convertToNativeCustomTypeSchema()
                modelProvider.addCustomTypeSchema(
                    it.name,
                    nativeSchema
                )
            }
        }
    }

    private fun createErrorHandler(request: Map<String, Any>): DataStoreErrorHandler {
        return if (request["hasErrorHandler"] as? Boolean? == true) {
            DataStoreErrorHandler {
                val args = mapOf(
                    "errorCode" to "DataStoreException",
                    "errorMessage" to ExceptionMessages.defaultFallbackExceptionMessage,
                    "details" to createSerializedError(it)
                )
                channel.invokeMethod("errorHandler", args)
            }
        } else {
            DataStoreErrorHandler {
                LOG.error(it.toString())
            }
        }
    }

    private fun createConflictHandler(request: Map<String, Any>): DataStoreConflictHandler {
        return if (request["hasConflictHandler"] as? Boolean? == true) {
            DataStoreConflictHandler { conflictData,
                onDecision ->

                val modelName = conflictData.local.modelName
                val args = mapOf(
                    "modelName" to modelName,
                    "local" to FlutterSerializedModel(conflictData.local as SerializedModel).toMap(),
                    "remote" to FlutterSerializedModel(conflictData.remote as SerializedModel).toMap()
                )

                uiThreadHandler.post {
                    channel.invokeMethod(
                        "conflictHandler",
                        args,
                        object : Result {
                            override fun success(result: Any?) {
                                val resultMap: Map<String, Any>? = result.safeCastToMap()
                                try {
                                    var resolutionStrategy: ResolutionStrategy = ResolutionStrategy.APPLY_REMOTE
                                    when (resultMap?.get("resolutionStrategy") as String) {
                                        "applyRemote" -> resolutionStrategy = ResolutionStrategy.APPLY_REMOTE
                                        "retryLocal" -> resolutionStrategy = ResolutionStrategy.RETRY_LOCAL
                                        "retry" -> resolutionStrategy = ResolutionStrategy.RETRY
                                    }
                                    when (resolutionStrategy) {
                                        ResolutionStrategy.APPLY_REMOTE -> onDecision.accept(DataStoreConflictHandler.ConflictResolutionDecision.applyRemote())
                                        ResolutionStrategy.RETRY_LOCAL -> onDecision.accept(DataStoreConflictHandler.ConflictResolutionDecision.retryLocal())
                                        ResolutionStrategy.RETRY -> {
                                            val serializedModel = SerializedModel.builder()
                                                .modelSchema(modelProvider.modelSchemas().getValue(modelName))
                                                .serializedData((resultMap["customModel"] as Map<*, *>).cast())
                                                .build()
                                            onDecision.accept(
                                                DataStoreConflictHandler.ConflictResolutionDecision.retry(
                                                    serializedModel
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    LOG.error("Unrecognized resolutionStrategy to resolve conflict. Applying default conflict resolution, applyRemote.")
                                    onDecision.accept(DataStoreConflictHandler.ConflictResolutionDecision.applyRemote())
                                }
                            }

                            override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                                LOG.error("Error in conflict handler: $errorCode $errorMessage Applying default conflict resolution, applyRemote.")
                                onDecision.accept(DataStoreConflictHandler.ConflictResolutionDecision.applyRemote())
                            }

                            override fun notImplemented() {
                                LOG.error("Conflict handler not implemented.  Applying default conflict resolution, applyRemote.")
                                onDecision.accept(DataStoreConflictHandler.ConflictResolutionDecision.applyRemote())
                            }
                        }
                    )
                }
            }
        } else {
            DataStoreConflictHandler { _,
                onDecision ->
                onDecision.accept(DataStoreConflictHandler.ConflictResolutionDecision.applyRemote())
            }
        }
    }

    override fun addAuthPlugin(callback: (kotlin.Result<Unit>) -> Unit) {
        try {
            Amplify.addPlugin(NativeAuthPluginWrapper { nativeAuthPlugin })
            LOG.info("Added Auth plugin")
            callback(kotlin.Result.success(Unit))
        } catch (e: Exception) {
            LOG.error(e.message)
            callback(kotlin.Result.failure(e))
        }
    }

    override fun updateCurrentUser(user: NativeAuthUser?) {
        currentUser = user?.let { AuthUser(it.userId, it.username) }
    }

    override fun addApiPlugin(
        authProvidersList: List<String>,
        endpoints: Map<String, String>,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        try {
            val authProviders = authProvidersList.map { AuthorizationType.valueOf(it) }
            if (flutterAuthProviders == null) {
                flutterAuthProviders = FlutterAuthProviders(authProviders, nativeApiPlugin!!)
            }
            Amplify.addPlugin(
                AWSApiPlugin
                    .builder()
                    .apiAuthProviders(flutterAuthProviders!!.factory)
                    .build()
            )
            LOG.info("Added API plugin")
            callback(kotlin.Result.success(Unit))
        } catch (e: Exception) {
            LOG.error(e.message)
            callback(kotlin.Result.failure(e))
        }
    }
    
    override fun sendSubscriptionEvent(
        event: NativeGraphQLSubscriptionResponse,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        throw NotImplementedError("Not yet implemented")
    }

    fun addUserAgent(
        version: String,
    ) {
        if(hasAddedUserAgent) return

        @OptIn(AmplifyFlutterApi::class)
        Amplify.addUserAgentPlatform(UserAgent.Platform.FLUTTER, "$version /datastore")
        
        hasAddedUserAgent = true
    }
    
    override fun configure(
        version: String,
        config: String,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        coroutineScope.launch(dispatcher) {
            try {
                addUserAgent(version)
                Amplify.configure(AmplifyOutputs(config), context)

                withContext(Dispatchers.Main) {
                    callback(kotlin.Result.success(Unit))
                }
            } catch (e: Amplify.AlreadyConfiguredException) {
                val flutterError = FlutterError(
                    "AmplifyAlreadyConfiguredException",
                    e.toString()
                )
                callback(kotlin.Result.failure(flutterError))
            } catch (e: AmplifyException) {
                val flutterError = FlutterError(
                    "AmplifyException",
                    e.toString()
                )
                callback(kotlin.Result.failure(flutterError))
            }
        }
    }
}
