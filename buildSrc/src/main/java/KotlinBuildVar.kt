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
    private const val KUIKLY_VERSION = "2.24.0"

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

    /**
     * 发布坐标的 artifactId：与 Gradle 模块名保持一致
     * （对齐 Kuikly 生态惯例，如 `KuiklyWebview` / `KuiklyWebviewAndroid`）。
     *
     * 消费者实际声明：
     * - `io.github.arialentropy:KuiklyNotification:<version>`        （Kuikly 侧公共 API，Android 变体自动匹配）
     * - `io.github.arialentropy:KuiklyNotificationAndroid:<version>` （Android 原生实现）
     */
    const val KMP_ARTIFACT_ID = "KuiklyNotification"
    const val ANDROID_ARTIFACT_ID = "KuiklyNotificationAndroid"

    // ---------------- POM 元数据（Maven 仓库通用） ----------------

    const val LIB_NAME = "KuiklyNotification"
    const val LIB_DESCRIPTION =
        "Cross-platform local notification component for Kuikly (Android / iOS / HarmonyOS)."
    const val LIB_URL = "https://github.com/AriaLEntropy/KuiklyNotification"
    const val LICENSE_NAME = "Apache-2.0"
    const val LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
    const val DEVELOPER_ID = "AriaLEntropy"
    const val DEVELOPER_NAME = "AriaLEntropy"

    /**
     * 发布目标一：仓库内本地 Maven 目录（默认）。
     * 把它推到 `gh-pages` 即成为 GitHub Pages Maven 仓库，消费者用
     * `maven { url = uri("https://<user>.github.io/KuiklyNotification/maven-repo") }` 即可拉取，无需任何账号。
     */
    const val LOCAL_REPO_DIR = "maven-repo"

    /**
     * 发布目标二（可选）：Maven Central。
     * 需在 `local.properties` 提供 `username` / `password`（Sonatype 凭据）并配置 GPG 签名；
     * 未提供凭据时不会创建该仓库，脚本可安全地在两种模式下运行。
     */
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

/**
 * 发布相关的公共逻辑，供各模块 build.gradle.kts 复用：
 * - 统一填充 POM 元数据（Maven 仓库 / Central 通用）
 * - 按需创建发布仓库：本地 maven-repo（默认）+ 可选 Central（有凭据才创建）
 */
object Publishing {

    /** 统一填充 POM 元数据 */
    fun applyPom(
        publication: org.gradle.api.publish.maven.MavenPublication,
        project: org.gradle.api.Project
    ) {
        publication.pom {
            name.set(MavenConfig.LIB_NAME)
            description.set(MavenConfig.LIB_DESCRIPTION)
            url.set(MavenConfig.LIB_URL)
            licenses {
                license {
                    name.set(MavenConfig.LICENSE_NAME)
                    url.set(MavenConfig.LICENSE_URL)
                }
            }
            developers {
                developer {
                    id.set(MavenConfig.DEVELOPER_ID)
                    name.set(MavenConfig.DEVELOPER_NAME)
                }
            }
            scm {
                url.set(MavenConfig.LIB_URL)
                connection.set("scm:git:${MavenConfig.LIB_URL}.git")
                developerConnection.set("scm:git:${MavenConfig.LIB_URL}.git")
            }
        }
    }

    /**
     * 注册发布仓库。
     * - 始终注册 `localRepo` → 仓库根目录的 `maven-repo/`（可推到 gh-pages 当 Maven 仓库）
     * - 仅当 `local.properties` 提供 sonatype 凭据时，才注册 `central`
     */
    fun registerRepositories(
        repositoryHandler: org.gradle.api.artifacts.dsl.RepositoryHandler,
        project: org.gradle.api.Project
    ) {
        val localRepoDir = project.rootProject.layout.projectDirectory
            .dir(MavenConfig.LOCAL_REPO_DIR).asFile.toURI()
        repositoryHandler.maven {
            name = "localRepo"
            url = localRepoDir
        }

        val repoUser = MavenConfig.getUsername(project)
        val repoPassword = MavenConfig.getPassword(project)
        if (repoUser.isNotEmpty() && repoPassword.isNotEmpty()) {
            val centralUrl = java.net.URI(MavenConfig.getRepoUrl(MavenConfig.VERSION))
            repositoryHandler.maven {
                name = "central"
                url = centralUrl
                credentials {
                    username = repoUser
                    password = repoPassword
                }
            }
        }
    }
}
