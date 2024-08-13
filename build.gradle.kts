import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.graalvm)
}

graalvmNative {
    toolchainDetection = true
    binaries {
            named("main") {
                javaLauncher = javaToolchains.launcherFor {
                    vendor = JvmVendorSpec.matching("graalvm")
                    languageVersion = JavaLanguageVersion.of(22)
                }
                imageName = "openid-helper"
                mainClass = "dev.schlaubi.openid.helper.MainKt"
                if(HostManager.hostIsLinux) {
                    buildArgs = listOf("--static", "--libc=musl")
                }
            }
    }
}
