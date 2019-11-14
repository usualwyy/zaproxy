import japicmp.model.JApiChangeStatus
import java.time.LocalDate
import java.util.stream.Collectors
import me.champeau.gradle.japicmp.JapicmpTask
import org.zaproxy.zap.tasks.GradleBuildWithGitRepos
import org.zaproxy.zap.japicmp.AcceptMethodAbstractNowDefaultRule

plugins {
    `java-library`
    jacoco
    id("me.champeau.gradle.japicmp")
    org.zaproxy.zap.distributions
    org.zaproxy.zap.installers
    org.zaproxy.zap.`github-releases`
    org.zaproxy.zap.publish
    org.zaproxy.zap.spotless
}

group = "org.zaproxy"
version = "2.9.0-SNAPSHOT"
val versionBC = "2.8.0"

val versionLangFile = "1"
val creationDate by extra { project.findProperty("creationDate") ?: LocalDate.now().toString() }
val distDir = file("src/main/dist/")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jacoco {
    toolVersion = "0.8.4"
}

dependencies {
    api("com.fifesoft:rsyntaxtextarea:3.0.4")
    api("com.github.zafarkhaja:java-semver:0.9.0")
    api("commons-beanutils:commons-beanutils:1.9.4")
    api("commons-codec:commons-codec:1.13")
    api("commons-collections:commons-collections:3.2.2")
    api("commons-configuration:commons-configuration:1.10")
    api("commons-httpclient:commons-httpclient:3.1")
    api("commons-io:commons-io:2.6")
    api("commons-lang:commons-lang:2.6")
    api("org.apache.commons:commons-lang3:3.9")
    api("org.apache.commons:commons-text:1.8")
    api("edu.umass.cs.benchlab:harlib:1.1.2")
    api("javax.help:javahelp:2.0.05")
    api("log4j:log4j:1.2.17")
    api("net.htmlparser.jericho:jericho-html:3.4")
    api("net.sf.json-lib:json-lib:2.4:jdk15")
    api("org.apache.commons:commons-csv:1.7")
    api("org.bouncycastle:bcmail-jdk15on:1.64")
    api("org.bouncycastle:bcprov-jdk15on:1.64")
    api("org.bouncycastle:bcpkix-jdk15on:1.64")
    api("org.hsqldb:hsqldb:2.5.0")
    api("org.jfree:jfreechart:1.0.19")
    api("org.jgrapht:jgrapht-core:0.9.0")
    api("org.swinglabs.swingx:swingx-all:1.6.5-1")
    api("org.xerial:sqlite-jdbc:3.28.0")

    implementation("commons-validator:commons-validator:1.6")
    // Don't need its dependencies, for now.
    implementation("org.jitsi:ice4j:1.0") {
        setTransitive(false)
    }
    implementation("org.javadelight:delight-nashorn-sandbox:0.1.26")

    runtimeOnly("commons-jxpath:commons-jxpath:1.3")
    runtimeOnly("commons-logging:commons-logging:1.2")
    runtimeOnly("com.io7m.xom:xom:1.2.10") {
        setTransitive(false)
    }

    testImplementation("com.github.tomakehurst:wiremock-jre8:2.25.1")
    testImplementation("junit:junit:4.11")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.mockito:mockito-core:3.1.0")
    testImplementation("org.slf4j:slf4j-log4j12:1.7.28")

    testRuntimeOnly(files(distDir))
}

tasks.register<JavaExec>("run") {
    group = ApplicationPlugin.APPLICATION_GROUP
    description = "Runs ZAP from source, using the default dev home."

    main = "org.zaproxy.zap.ZAP"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = distDir
}

listOf("jar", "jarDaily").forEach {
    tasks.named<Jar>(it) {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirMode = "0755".toIntOrNull(8)
        fileMode = "0644".toIntOrNull(8)

        val attrs = mapOf(
                "Main-Class" to "org.zaproxy.zap.ZAP",
                "Implementation-Version" to ToString({ archiveVersion.get() }),
                "Create-Date" to creationDate,
                "Class-Path" to ToString({ configurations.runtimeClasspath.get().files.stream().map { file -> "lib/${file.name}" }.sorted().collect(Collectors.joining(" ")) }))

        manifest {
            attributes(attrs)
        }
    }
}

val japicmp by tasks.registering(JapicmpTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks ${project.name}.jar binary compatibility with latest version ($versionBC)."

    oldClasspath = files(zapJar(versionBC))
    newClasspath = files(tasks.named<Jar>(JavaPlugin.JAR_TASK_NAME).map { it.archivePath })
    ignoreMissingClasses = true
    packageExcludes = listOf(
        // Should no longer be in use by any add-on
        "org.parosproxy.paros.extension.filter"
    )
    classExcludes = listOf(
        // Not expected to be used by add-ons
        "org.parosproxy.paros.view.LicenseFrame",
        "org.zaproxy.zap.view.LicenseFrame"
    )
    fieldExcludes = listOf(
        // Not expected to be used by add-ons
        "org.parosproxy.paros.Constant#ACCEPTED_LICENSE",
        "org.parosproxy.paros.Constant#ACCEPTED_LICENSE_DEFAULT"
    )
    methodExcludes = listOf(
        // Implementation moved to interface
        "org.parosproxy.paros.extension.ExtensionAdaptor#getURL()",
        "org.parosproxy.paros.extension.ExtensionAdaptor#getAuthor()",
        // Not expected to be used by add-ons
        "org.zaproxy.zap.extension.autoupdate.ExtensionAutoUpdate#getLatestVersionInfo(org.zaproxy.zap.extension.autoupdate.CheckForUpdateCallback)",
        "org.zaproxy.zap.extension.autoupdate.ManageAddOnsDialog#checkForUpdates()"
    )

    richReport {
        destinationDir = file("$buildDir/reports/japicmp/")
        reportName = "japi.html"
        isAddDefaultRules = true
        addRule(JApiChangeStatus.MODIFIED, AcceptMethodAbstractNowDefaultRule::class.java)
    }
}

tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn(japicmp)
}

tasks.named<Javadoc>("javadoc") {
    title = "OWASP Zed Attack Proxy"
    source = sourceSets["main"].allJava.matching {
        include("org/parosproxy/**")
        include("org/zaproxy/**")
    }
    (options as StandardJavadocDocletOptions).run {
        links("https://docs.oracle.com/javase/8/docs/api/")
        encoding = "UTF-8"
        source("${java.targetCompatibility}")
    }
}

val langPack by tasks.registering(Zip::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Assembles the language pack for the Core Language Files add-on."

    archiveFileName.set("$buildDir/langpack/ZAP_${project.version}_language_pack.$versionLangFile.zaplang")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    into("lang") {
        from(File(distDir, "lang"))
        from("src/main/resources/org/zaproxy/zap/resources") {
            include("Messages.properties", "vulnerabilities.xml")
        }
    }
}

tasks.register<Copy>("copyLangPack") {
    group = "ZAP Misc"
    description = "Copies the language pack into the Core Language Files add-on (assumes zap-extensions repo is in same directory as zaproxy)."

    from(langPack)
    into("$rootDir/../zap-extensions/addOns/coreLang/src/main/zapHomeFiles/lang/")
}

val copyWeeklyAddOns by tasks.registering(GradleBuildWithGitRepos::class) {
    group = "ZAP Misc"
    description = "Copies the weekly add-ons into plugin dir, built from local repos."

    repositoriesDirectory.set(rootDir.parentFile)
    repositoriesDataFile.set(file("src/main/weekly-add-ons.json"))
    cloneRepositories.set(false)
    updateRepositories.set(false)

    val outputDir = file("src/main/dist/plugin/")
    tasks {
        register("copyZapAddOn") {
            args.set(listOf("--into=$outputDir"))
        }
    }
}

val generateAllApiEndpoints by tasks.registering {
    group = "ZAP Misc"
    description = "Generates (and copies) the ZAP API endpoints for all languages."
}

listOf(
    "org.zaproxy.zap.extension.api.GoAPIGenerator",
    "org.zaproxy.zap.extension.api.JavaAPIGenerator",
    "org.zaproxy.zap.extension.api.NodeJSAPIGenerator",
    "org.zaproxy.zap.extension.api.PhpAPIGenerator",
    "org.zaproxy.zap.extension.api.PythonAPIGenerator",
    "org.zaproxy.zap.extension.api.RustAPIGenerator",
    "org.zaproxy.zap.extension.api.WikiAPIGenerator"
).forEach {
    val langName = it.removePrefix("org.zaproxy.zap.extension.api.").removeSuffix("APIGenerator")
    val task = tasks.register<JavaExec>("generate${langName}ApiEndpoints") {
        group = "ZAP Misc"
        description = "Generates (and copies) the ZAP API endpoints for $langName."

        main = it
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = file("$rootDir")
    }

    generateAllApiEndpoints {
        dependsOn(task)
    }
}

launch4j {
    jar = tasks.named<Jar>("jar").get().archiveFileName.get()
}

class ToString(private val callable: Callable<String>) {
    override fun toString() = callable.call()
}

fun zapJar(version: String): File {
    val oldGroup = group
    try {
        // https://discuss.gradle.org/t/is-the-default-configuration-leaking-into-independent-configurations/2088/6
        group = "virtual_group_for_japicmp"
        val conf = configurations.detachedConfiguration(dependencies.create("$oldGroup:$name:$version"))
        conf.isTransitive = false
        return conf.singleFile
    } finally {
        group = oldGroup
    }
}
