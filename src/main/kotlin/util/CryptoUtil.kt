package dev.schlaubi.openid.helper.util

import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
fun String.md5(): String {
    val md5 = MessageDigest.getInstance("MD5")
    return md5.digest(toByteArray()).toHexString()
}
