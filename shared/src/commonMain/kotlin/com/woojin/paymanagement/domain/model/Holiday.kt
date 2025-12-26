package com.woojin.paymanagement.domain.model

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

object ItemsSerializer : KSerializer<HolidayApiResponse.Items?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Items")

    override fun deserialize(decoder: Decoder): HolidayApiResponse.Items? {
        return try {
            val element = (decoder as JsonDecoder).decodeJsonElement()
            if (element is JsonObject) {
                decoder.json.decodeFromJsonElement(HolidayApiResponse.Items.serializer(), element)
            } else {
                null // empty string case
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: HolidayApiResponse.Items?) {
        if (value != null) {
            encoder.encodeSerializableValue(HolidayApiResponse.Items.serializer(), value)
        } else {
            encoder.encodeString("")
        }
    }
}

object ItemListSerializer : KSerializer<List<HolidayApiResponse.Item>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ItemList")

    override fun deserialize(decoder: Decoder): List<HolidayApiResponse.Item>? {
        return try {
            val element = (decoder as JsonDecoder).decodeJsonElement()
            when (element) {
                is JsonArray -> {
                    // 배열인 경우: 그대로 리스트로 파싱
                    decoder.json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(HolidayApiResponse.Item.serializer()),
                        element
                    )
                }
                is JsonObject -> {
                    // 단일 객체인 경우: 리스트로 감싸서 반환
                    val item = decoder.json.decodeFromJsonElement(HolidayApiResponse.Item.serializer(), element)
                    listOf(item)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: List<HolidayApiResponse.Item>?) {
        if (value != null) {
            encoder.encodeSerializableValue(
                kotlinx.serialization.builtins.ListSerializer(HolidayApiResponse.Item.serializer()),
                value
            )
        }
    }
}

@Serializable
data class HolidayApiResponse(
    val response: Response
) {
    @Serializable
    data class Response(
        val header: Header,
        val body: Body
    )

    @Serializable
    data class Header(
        val resultCode: String,
        val resultMsg: String
    )

    @Serializable
    data class Body(
        @Serializable(with = ItemsSerializer::class)
        val items: Items?,
        val numOfRows: Int,
        val pageNo: Int,
        val totalCount: Int
    )

    @Serializable
    data class Items(
        @Serializable(with = ItemListSerializer::class)
        val item: List<Item>?
    )

    @Serializable
    data class Item(
        val locdate: String,        // YYYYMMDD
        val seq: Int?,
        val dateKind: String?,
        val isHoliday: String,      // Y 또는 N
        val dateName: String        // 공휴일 이름
    )
}

data class Holiday(
    val locdate: String,        // YYYYMMDD
    val dateName: String,       // 공휴일 이름
    val isHoliday: Boolean,     // true면 공휴일
    val year: Int               // 연도
)
