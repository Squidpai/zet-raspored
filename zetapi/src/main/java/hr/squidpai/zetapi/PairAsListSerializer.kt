package hr.squidpai.zetapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> PairAsListSerializer(
    elementSerializer: KSerializer<T>
) = PairAsListSerializer(T::class as KClass<T & Any>, elementSerializer)

@OptIn(ExperimentalSerializationApi::class)
internal class PairAsListSerializer<T>(
    kClass: KClass<T & Any>,
    elementSerializer: KSerializer<T>
) : KSerializer<Pair<T, T>> {
    private val delegateSerializer = ArraySerializer(kClass, elementSerializer)

    override val descriptor = SerialDescriptor(
        "kotlin.Pair<T, T>",
        delegateSerializer.descriptor
    )

    override fun serialize(encoder: Encoder, value: Pair<T, T>) =
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableValue(
            delegateSerializer,
            arrayOf<Any?>(value.first, value.second) as Array<T>,
        )

    override fun deserialize(decoder: Decoder): Pair<T, T> {
        val data = decoder.decodeSerializableValue(delegateSerializer)
        return data[0] to data[1]
    }
}