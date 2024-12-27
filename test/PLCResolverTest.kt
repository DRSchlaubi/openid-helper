import dev.schlaubi.openid.helper.providers.implementations.bluesky.InvalidHandleException
import dev.schlaubi.openid.helper.providers.implementations.bluesky.resolveDid
import dev.schlaubi.openid.helper.providers.implementations.bluesky.resolveDomainName
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PLCResolverTest {

    @Test
    fun testTxtDomainResolve() = runTest {
        val (server, _, hint) = resolveDomainName("schlau.bi")

        assertEquals("https://am.schlau.bi", server.issuer)
        assertEquals("schlau.bi", hint)
    }

    // > If the auth flow instead starts with a server (hostname or URL),
    // > the client will first attempt to fetch Resource Server metadata
    // https://docs.bsky.app/docs/advanced-guides/oauth-client#account-or-server-identifier
    @Test
    fun testPDSResolve() = runTest {
        val (server, _, hint) = resolveDomainName("am.schlau.bi")

        assertEquals("https://am.schlau.bi", server.issuer)
        assertNull(hint)
    }

    @Test
    fun testHttpDomainResolve() = runTest {
        val (server, _, hint) = resolveDomainName("jaz.bsky.social")

        assertEquals("https://bsky.social", server.issuer)
        assertEquals("jaz.bsky.social", hint)
    }

    @Test
    fun testResolveDid() = runTest {
        val (server, _, hint) = resolveDid("did:plc:ewvi7nxzyoun6zhxrhs64oiz")

        assertEquals("https://bsky.social", server.issuer)
        assertNull(hint)
    }

    @Test
    fun testResolveDidWeb() = runTest {
        val (server, _, hint) = resolveDid("did:web:krasovs.ky")

        assertEquals("https://pds.krasovs.ky", server.issuer)
        assertNull(hint)
    }

    @Test
    fun testFailWithInvalidDomain() = runTest {
        assertFailsWith<InvalidHandleException> { resolveDomainName("google.de") }
    }

    @Test
    fun testFailWithInvalidDid() = runTest {
        assertFailsWith<InvalidHandleException> { resolveDid("dasdsadsadsa") }
    }
}