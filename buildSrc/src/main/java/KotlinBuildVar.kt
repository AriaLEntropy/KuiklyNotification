import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * 版本与坐标集中配置处。
 * ⚠️ Kuikly / Kotlin 版本如与实际发布版本不一致，只需改这里。
 */
object Version {

    /** Kuikly 主版本 */
    private const val KUIKLY_VERSION = "2.23.2"

    /** Kotlin 版本 */
    private const val KOTLIN_VERSION = "2.1.21"

    /** Kotlin 鸿蒙版本 */
    private const val KOTLIN_OHOS_VERSION = "2.0.21-ohos"

    /** Kuikly 通用版本号：${主版本}-${Kotlin 版本} */
    fun getKuiklyVersion(): String = "$KUIKLY_VERSION-$KOTLIN_VERSION"

    /** Kuikly 鸿蒙版本号 */
    fun getKuiklyOhosVersion(): String = "$KUIKLY_VERSION-$KOTLIN_OHOS_VERSION"
}

object BuildPlugin {
    /** Kuikly Gradle 插件坐标 */
    val kuikly by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:${Version.getKuiklyVersion()}"
    }
}

object MavenConfig {
    const val GROUP = "io.github.arialentropy"
    const val VERSION = "1.0.0"
    const val VERSION_OHOS = "$VERSION-ohos"

    const val KMP_ARTIFACT_ID = "kuikly-notification"
    const val ANDROID_ARTIFACT_ID = "kuikly-notification-android"

    private const val REPO_URL =
        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2"
    private const val SNAPSHOT_REPO_URL =
        "https://central.sonatype.com/repository/maven-snapshots/"

    private const val KEY_USER_NAME = "username"
    private const val KEY_USER_PASSWORD = "password"

    fun getUsername(project: Project): String {
        (project.findProperty(KEY_USER_NAME) as? String)?.takeIf { it.isNotEmpty() }?.let { return it }
        val file = File(project.rootDir, "local.properties")
        if (!file.exists()) return ""
        return Properties().apply { load(FileInputStream(file)) }.getProperty(KEY_USER_NAME) ?: ""
    }

    fun getPassword(project: Project): String {
        (project.findProperty(KEY_USER_PASSWORD) as? String)?.takeIf { it.isNotEmpty() }?.let { return it }
        val file = File(project.rootDir, "local.properties")
        if (!file.exists()) return ""
        return Properties().apply { load(FileInputStream(file)) }.getProperty(KEY_USER_PASSWORD) ?: ""
    }

    fun getRepoUrl(version: String): String =
        if (version.endsWith("-SNAPSHOT")) SNAPSHOT_REPO_URL else REPO_URL
}
