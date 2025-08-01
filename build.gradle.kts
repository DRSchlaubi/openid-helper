plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.io)
    implementation(libs.kotlinx.serialization.json.io)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.resources)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.stdx.envconf)
    implementation(libs.slf4j.simple)
    implementation(libs.kotlin.logging)
    implementation(libs.java.jwt)
    implementation(libs.nimbus.jwt)
    implementation(libs.bouncycastle.bcprov.jdk18on)
    implementation(libs.bouncycastle.bcutil.jdk18on)
    implementation(libs.bouncycastle.bcpkix.jdk18on)
    implementation(libs.kord.cache.api)
    implementation(libs.kord.cache.map)
    implementation(libs.kord.cache.redis)
    implementation(libs.doh)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.engine)
}
