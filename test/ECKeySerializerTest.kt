import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import dev.schlaubi.openid.helper.util.ECKeySerializer
import dev.schlaubi.openid.helper.util.SerializableECKey
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class ECKeySerializerTest {

    @Test
    fun testSerializer() {
        val key: SerializableECKey = ECKeyGenerator(Curve.P_256).generate()
        val verifier = ECDSAVerifier(key)

        val keySerialized = Json.encodeToString(ECKeySerializer, key)
        val keyDeserialized = Json.decodeFromString(ECKeySerializer, keySerialized)
        val signer = ECDSASigner(keyDeserialized)

        val claims = JWTClaimsSet.Builder().build()
        val header = JWSHeader.Builder(JWSAlgorithm.ES256).build()
        val jwt = SignedJWT(header, claims).apply {
            sign(signer)
        }.serialize()

        val parsedJwt = SignedJWT.parse(jwt)
        assertTrue { parsedJwt.verify(verifier) }
    }
}