package dev.schlaubi.openid.helper.util

import com.nimbusds.jose.jwk.ECKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableECKey = @Serializable(ECKeySerializer::class) ECKey

object ECKeySerializer : KSerializer<ECKey> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ECKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ECKey) = encoder.encodeString(value.toJSONString())

    override fun deserialize(decoder: Decoder): ECKey = ECKey.parse(decoder.decodeString())
}
