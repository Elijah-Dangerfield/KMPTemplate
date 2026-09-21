#!/usr/bin/env kotlin

// No @file:DependsOn here on purpose: the script uses only the stdlib, and a
// dependency declaration makes every cold run resolve Maven coordinates over
// the network (observed hanging the script runner entirely on flaky daemons).

import java.io.File
import kotlin.system.exitProcess

/**
 * KMP Template Project Initialization Script
 *
 * This script helps you set up a new project from the KMP Template.
 * It will rename all template placeholders to your chosen project name.
 *
 * Interactive usage: ./init_project.main.kts
 *
 * The script will prompt you for:
 * - App name (e.g., "My Awesome App") - used for display
 * - Package name (e.g., "com.example.myawesomeapp") - used for package declarations
 * - Contact email and destination directory
 *
 * Non-interactive usage (all flags required together — used by
 * scripts/verify_template.sh and template CI):
 *
 *   ./init_project.main.kts \
 *     --name "My App" --package com.example.myapp \
 *     --email you@example.com --dir /path/to/new/project \
 *     --ci=yes --backend=yes --yes
 *
 * Any flag present requires ALL of them; exit code is non-zero on any
 * validation or copy failure so automation can gate on it.
 */

// Color codes for terminal output
private val RED = "\u001b[31m"
private val GREEN = "\u001b[32m"
private val YELLOW = "\u001b[33m"
private val BLUE = "\u001b[34m"
private val CYAN = "\u001b[36m"
private val RESET = "\u001b[0m"

fun printRed(text: String) = println("$RED$text$RESET")
fun printGreen(text: String) = println("$GREEN$text$RESET")
fun printYellow(text: String) = println("$YELLOW$text$RESET")
fun printBlue(text: String) = println("$BLUE$text$RESET")
fun printCyan(text: String) = println("$CYAN$text$RESET")

/**
 * Represents different naming conventions for the project name.
 * Given an input like "My Awesome App":
 * - pascalCase: "MyAwesomeApp"
 * - camelCase: "myAwesomeApp" 
 * - lowercase: "myawesomeapp"
 * - kebabCase: "my-awesome-app"
 * - snakeCase: "my_awesome_app"
 * - dotCase: "my.awesome.app"
 * - displayName: "My Awesome App"
 */
data class ProjectName(
    val displayName: String,      // "My Awesome App"
    val pascalCase: String,       // "MyAwesomeApp"
    val camelCase: String,        // "myAwesomeApp"
    val lowercase: String,        // "myawesomeapp"
    val kebabCase: String,        // "my-awesome-app"
    val snakeCase: String,        // "my_awesome_app"
    val dotCase: String           // "my.awesome.app"
) {
    companion object {
        /**
         * Creates ProjectName from a display name like "My Awesome App"
         */
        fun fromDisplayName(displayName: String): ProjectName {
            val words = displayName.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
            
            val pascalCase = words.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            val camelCase = words.mapIndexed { index, word ->
                if (index == 0) word.lowercase() else word.replaceFirstChar { it.uppercase() }
            }.joinToString("")
            val lowercase = words.joinToString("") { it.lowercase() }
            val kebabCase = words.joinToString("-") { it.lowercase() }
            val snakeCase = words.joinToString("_") { it.lowercase() }
            val dotCase = words.joinToString(".") { it.lowercase() }
            
            return ProjectName(
                displayName = displayName,
                pascalCase = pascalCase,
                camelCase = camelCase,
                lowercase = lowercase,
                kebabCase = kebabCase,
                snakeCase = snakeCase,
                dotCase = dotCase
            )
        }
        
        /**
         * Creates ProjectName from a PascalCase identifier like "MyAwesomeApp"
         */
        fun fromPascalCase(pascalCase: String): ProjectName {
            // Split PascalCase into words
            val words = pascalCase.replace(Regex("([a-z])([A-Z])"), "$1 $2").split(" ")
            val displayName = words.joinToString(" ")
            return fromDisplayName(displayName)
        }
    }
}

// Template placeholders - these are what we search for and replace
val TEMPLATE_NAME = ProjectName(
    displayName = "KMP Template",
    pascalCase = "KMPTemplate",
    camelCase = "kmpTemplate",
    lowercase = "kmptemplate",
    kebabCase = "kmp-template",
    snakeCase = "kmp_template",
    dotCase = "kmp.template"
)

// Old package prefix to replace
val TEMPLATE_PACKAGE = "com.kmptemplate"

// Extensions to process for content replacement
val TEXT_FILE_EXTENSIONS = setOf(
    "kt", "kts", "java", "xml", "json", "yaml", "yml", "md", "txt",
    "properties", "gradle", "swift", "h", "m", "plist", "entitlements",
    "xcconfig", "pbxproj", "xcscheme", "storyboard", "xib",
    // Server: fly.toml app name + version catalog. (Dockerfile/.env are
    // extensionless and server config is project-agnostic, so they don't
    // carry the project name.)
    "toml", "sql",
    // Staged web content, if a project ever adds any. The legal Markdown that
    // carries {{APP_NAME}} and {{APP_SLUG}} is covered by "md" above.
    "html", "css",
    // R8 keep rules are written against the package prefix. Left un-renamed,
    // every `-keep class com.kmptemplate.**` matches nothing in the generated
    // project — R8 then strips serializers, routes and the DI entry point, and
    // the build still succeeds. The failure is at runtime, in release only.
    "pro"
)

// Directories to skip during processing AND during copy
val SKIP_DIRECTORIES = setOf(
    ".git", ".gradle", ".idea", "build", "node_modules", ".kotlin",
    "caches", "generated", "intermediates",
    // Machine-local agent settings — never ship into generated projects.
    ".claude"
)

// The template/ folder at the repo root is the init-time staging area
// (SETUP.md + ci/). It is copied explicitly — the wholesale copy skips it.
val TEMPLATE_STAGING_DIR = "template"

// Files to skip during content replacement
val SKIP_FILES = setOf(
    "init_project.main.kts",
    "gradlew", "gradlew.bat",
    ".DS_Store"
)

data class ReplacementStats(
    var filesModified: Int = 0,
    var foldersRenamed: Int = 0,
    var filesRenamed: Int = 0,
    var replacementsMade: Int = 0
)

/**
 * Values collected from CLI flags for the non-interactive mode. Null means
 * "no flags given — run interactively".
 */
data class CliConfig(
    val name: String,
    val packageName: String,
    val email: String,
    val dir: String,
    val ciEnabled: Boolean,
    val backendEnabled: Boolean
)

fun cliFail(message: String): Nothing {
    printRed("❌ $message")
    exitProcess(1)
}

/**
 * All-or-nothing flag parsing: any flag present requires --name, --package,
 * --email, --dir, --ci=yes|no, --backend=yes|no AND --yes. Deliberately flags
 * (not env vars or piped stdin) so automation breaks loudly when the interface
 * changes instead of silently answering the wrong prompt.
 */
fun parseCliConfig(rawArgs: List<String>): CliConfig? {
    if (rawArgs.isEmpty()) return null
    val values = mutableMapOf<String, String>()
    var confirmed = false
    var i = 0
    while (i < rawArgs.size) {
        val arg = rawArgs[i]
        when {
            arg == "--yes" -> { confirmed = true; i++ }
            arg.startsWith("--") && arg.contains('=') -> {
                values[arg.substringBefore('=').removePrefix("--")] = arg.substringAfter('=')
                i++
            }
            arg.startsWith("--") -> {
                if (i + 1 >= rawArgs.size) cliFail("Missing value for $arg")
                values[arg.removePrefix("--")] = rawArgs[i + 1]
                i += 2
            }
            else -> cliFail("Unexpected argument: $arg")
        }
    }

    val required = listOf("name", "package", "email", "dir", "ci", "backend")
    val missing = required.filterNot(values::containsKey)
    if (missing.isNotEmpty() || !confirmed) {
        val flags = missing.map { "--$it" } + if (confirmed) emptyList() else listOf("--yes")
        cliFail(
            "Non-interactive mode is all-or-nothing; missing: ${flags.joinToString(" ")}\n" +
                "   Usage: ./init_project.main.kts --name \"My App\" --package com.example.myapp " +
                "--email you@example.com --dir /path --ci=yes|no --backend=yes|no --yes"
        )
    }

    fun yesNo(flag: String): Boolean = when (values.getValue(flag)) {
        "yes" -> true
        "no" -> false
        else -> cliFail("--$flag must be yes or no")
    }

    return CliConfig(
        name = values.getValue("name"),
        packageName = values.getValue("package"),
        email = values.getValue("email"),
        dir = values.getValue("dir"),
        ciEnabled = yesNo("ci"),
        backendEnabled = yesNo("backend")
    )
}

fun main(cli: CliConfig?) {
    if (cli == null) {
        printBlue("""
            ╔══════════════════════════════════════════════════════════════╗
            ║         🚀 KMP Template Project Initialization 🚀            ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  Creates a fresh copy of the template with your project     ║
            ║  name — the original template is left untouched.            ║
            ╚══════════════════════════════════════════════════════════════╝
        """.trimIndent())
        println()
    }

    val projectName: ProjectName
    val packageName: String
    val contactEmail: String
    val projectDir: File

    if (cli != null) {
        projectName = validateProjectName(cli.name.trim()) ?: cliFail("Invalid --name: ${cli.name}")
        packageName = validatePackageName(cli.packageName.trim()) ?: cliFail("Invalid --package: ${cli.packageName}")
        contactEmail = cli.email.trim().ifEmpty { cliFail("--email must not be empty") }
        // A relative --dir resolves against the template's parent, not the
        // working directory — see [resolveDestination]. Checked before mkdirs so
        // a rejected path doesn't leave an empty directory behind.
        val dir = resolveDestination(cli.dir)
        destinationProblem(dir)?.let {
            cliFail("--dir must be outside the template: $it. Got: ${dir.absolutePath}")
        }
        if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) {
            cliFail("--dir already exists and is not empty: ${dir.absolutePath}")
        }
        if (!dir.exists() && !dir.mkdirs()) cliFail("Could not create --dir: ${dir.absolutePath}")
        projectDir = dir
    } else {
        projectName = getProjectName() ?: return
        packageName = getPackageName(projectName) ?: return
        contactEmail = getContactEmail() ?: return
        projectDir = getProjectDir(projectName) ?: return
    }

    println()
    printCyan("📋 Configuration Summary:")
    println("   Display Name:  ${projectName.displayName}")
    println("   PascalCase:    ${projectName.pascalCase}")
    println("   camelCase:     ${projectName.camelCase}")
    println("   lowercase:     ${projectName.lowercase}")
    println("   kebab-case:    ${projectName.kebabCase}")
    println("   Package:       $packageName")
    println("   Destination:   ${projectDir.absolutePath}")
    println()

    if (cli == null) {
        print("Proceed with these settings? (Y/n): ")
        val confirm = readln().trim().lowercase()
        if (confirm.isNotEmpty() && confirm != "y" && confirm != "yes") {
            printYellow("👋 Initialization cancelled. Run again when ready!")
            return
        }
    }

    println()
    printBlue("🔄 Starting project initialization...")

    val stats = ReplacementStats()
    val templateDir = File(".").canonicalFile

    try {
        printBlue("📋 Step 1/8: Copying template to ${projectDir.absolutePath}...")
        copyTemplate(templateDir, projectDir)
        printGreen("   ✓ Template copied")

        printBlue("📦 Step 2/8: Placing SETUP.md + configuring CI...")
        placeSetupDoc(templateDir, projectDir)
        val ciEnabled = maybeEnableCi(templateDir, projectDir, cli?.ciEnabled)
        // Asked now, applied in step 7. Every anchor the removal matches on is
        // project-agnostic, but SETUP.md and README.md are not — they go
        // through the rename pass first, so the edits have to run after it.
        val backendEnabled = wantsBackend(cli?.backendEnabled)

        printBlue("📝 Step 3/8: Replacing file contents...")
        replaceFileContents(projectDir, projectName, packageName, stats)

        printBlue("📁 Step 4/8: Renaming directories...")
        renameDirectories(projectDir, projectName, packageName, stats)

        printBlue("📄 Step 5/8: Renaming files...")
        renameFiles(projectDir, projectName, stats)

        printBlue("🔖 Step 6/8: Substituting CI placeholders...")
        substitutePlaceholders(projectDir, projectName, contactEmail)

        printBlue("🧹 Step 7/8: Cleaning up template artifacts...")
        cleanupTemplateArtifacts(projectDir, projectName, ciEnabled)
        if (!backendEnabled) removeBackend(projectDir, projectName)
        ensureExecutableBits(projectDir)

        printBlue("🔄 Step 8/8: Initializing git repository...")
        resetGitHistory(projectDir, projectName)

        println()
        printGreen("✅ Project initialization complete!")
        println()
        printCyan("📊 Summary:")
        println("   Files modified:     ${stats.filesModified}")
        println("   Folders renamed:    ${stats.foldersRenamed}")
        println("   Files renamed:      ${stats.filesRenamed}")
        println("   Total replacements: ${stats.replacementsMade}")
        println()
        printYellow("📍 Project created at: ${projectDir.absolutePath}")

        printNextSteps(projectDir, projectName, backendEnabled)
        maybeRunSetup(projectDir, cli != null)

    } catch (e: Exception) {
        printRed("❌ Error during initialization: ${e.message}")
        e.printStackTrace()
        printYellow("⚠️  Partially created project may exist at: ${projectDir.absolutePath}")
        printYellow("     The original template was not modified.")
        exitProcess(1)
    }
}

/**
 * Shared validation for both the interactive prompts and the CLI flags.
 * Returns null when the input is unusable.
 */
fun validateProjectName(input: String): ProjectName? {
    if (input.isEmpty()) return null
    val projectName = ProjectName.fromDisplayName(input)
    if (projectName.pascalCase.isEmpty() || !projectName.pascalCase[0].isLetter()) return null
    if (!projectName.pascalCase.all { it.isLetterOrDigit() }) return null
    return projectName
}

fun validatePackageName(input: String): String? {
    val packageRegex = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
    return if (packageRegex.matches(input)) input else null
}

fun copyTemplate(source: File, dest: File) {
    dest.mkdirs()
    source.listFiles()?.forEach { file ->
        if (file.name in SKIP_DIRECTORIES) return@forEach
        // template/ is a staging folder copied explicitly (SETUP.md always,
        // CI conditionally via enableCi()). Don't ship the staging folder
        // itself into the new project.
        if (file.parentFile?.canonicalPath == source.canonicalPath &&
            file.isDirectory && file.name == TEMPLATE_STAGING_DIR) return@forEach
        // The root .github/ is the TEMPLATE repo's own CI (template-ci.yml).
        // Generated projects get their workflows from template/ci/ instead.
        if (file.parentFile?.canonicalPath == source.canonicalPath &&
            file.isDirectory && file.name == ".github") return@forEach
        // Machine-specific (Android SDK path etc.) — the IDE regenerates it.
        if (file.parentFile?.canonicalPath == source.canonicalPath &&
            file.name == "local.properties") return@forEach
        val target = File(dest, file.name)
        if (file.isDirectory) {
            copyTemplate(file, target)
        } else {
            file.copyTo(target, overwrite = false)
            // File.copyTo doesn't preserve POSIX permissions. Restore the
            // executable bit for shell scripts and gradlew so the new project
            // is immediately runnable without `chmod +x`.
            if (file.canExecute()) {
                target.setExecutable(true, false)
            }
        }
    }
}

/**
 * Recursive file copy preserving directory structure. Skips SKIP_DIRECTORIES.
 */
fun copyRecursive(source: File, dest: File) {
    if (source.isDirectory) {
        dest.mkdirs()
        source.listFiles()?.forEach { child ->
            if (child.name in SKIP_DIRECTORIES) return@forEach
            copyRecursive(child, File(dest, child.name))
        }
    } else {
        dest.parentFile?.mkdirs()
        source.copyTo(dest, overwrite = true)
    }
}

/**
 * File.copyTo doesn't preserve the executable bit, so any shell scripts and
 * git hooks shipped in the template need +x applied explicitly after copy.
 */
fun ensureExecutableBits(projectDir: File) {
    val execPaths = listOf(
        "scripts/install_hooks.sh",
        "scripts/enable_ci.sh",
        "scripts/setup_legal_sync.sh",
        "scripts/cleanup.sh",
        "scripts/create_module.main.kts",
        "scripts/setup.main.kts",
        "scripts/setup_sentry.main.kts",
        "scripts/setup_supabase.main.kts",
        "scripts/setup_fly.main.kts",
        "scripts/setup_github_secrets.main.kts",
        "scripts/setup_credentials.main.kts",
        "scripts/lib/setup_store.main.kts",
        ".githooks/commit-msg",
        ".githooks/post-commit",
        ".githooks/pre-push",
        "gradlew"
    )
    execPaths.forEach { rel ->
        val f = File(projectDir, rel)
        if (f.exists()) f.setExecutable(true, false)
    }
}

fun placeSetupDoc(templateDir: File, projectDir: File) {
    val setupSrc = File(templateDir, "$TEMPLATE_STAGING_DIR/SETUP.md")
    if (!setupSrc.exists()) return
    val setupDst = File(projectDir, "SETUP.md")
    setupSrc.copyTo(setupDst, overwrite = true)
    printGreen("   ✓ Placed SETUP.md in project root")
}

/**
 * Asks whether to enable CI (or uses the --ci flag in non-interactive mode)
 * and, if yes, copies every file under template/ci/ into the project,
 * preserving relative paths.
 *
 * When CI is declined, the staging folder is still shipped (as template/ci/)
 * together with scripts/enable_ci.sh so the project can opt in later — the
 * staged files go through the same rename/substitution passes as everything
 * else, so enabling later is a pure file move.
 */
fun maybeEnableCi(templateDir: File, projectDir: File, cliCiEnabled: Boolean?): Boolean {
    val ciSrc = File(templateDir, "$TEMPLATE_STAGING_DIR/ci")
    if (!ciSrc.exists() || !ciSrc.isDirectory) return false

    val enable = cliCiEnabled ?: run {
        println()
        printCyan("""
            🚢 Enable CI / release automation?

            This copies release-please, fastlane, the legal documents, and the
            Sentry triage prompt into your project:

              • .github/workflows/*.yml  (ci, release-please, release, etc.)
              • apps/ios/Gemfile + apps/ios/fastlane/*
              • legal/privacy.md, legal/terms.md + the workflow that publishes
                them to the studio site
              • release-please-config.json, .release-please-manifest.json

            You'll still need to set GitHub secrets and create store listings
            before the pipeline will actually ship — see SETUP.md.

            Say no if you want to wire this up later (or never) — you can
            enable it any time by running ./scripts/enable_ci.sh.
        """.trimIndent())
        println()
        print("Enable CI? (y/N): ")
        val answer = readln().trim().lowercase()
        answer == "y" || answer == "yes"
    }

    if (!enable) {
        // Ship the staging folder so scripts/enable_ci.sh can install it later.
        copyRecursive(ciSrc, File(projectDir, "$TEMPLATE_STAGING_DIR/ci"))
        printYellow("   → CI skipped. Run ./scripts/enable_ci.sh later to install it.")
        return false
    }

    ciSrc.listFiles()?.forEach { child ->
        val dst = File(projectDir, child.name)
        copyRecursive(child, dst)
    }
    printGreen("   ✓ CI files installed")
    return true
}

/**
 * Asks whether this project has a backend of its own (or uses --backend).
 *
 * Deliberately not "does your app need a server". `:libraries:identity`
 * (Supabase auth) and `:libraries:networking` do not depend on `:apps:server`,
 * so a client-only app can still sign in against Supabase and call third-party
 * HTTP all day. The question is only about *this repo* shipping a server.
 *
 * Saying no is destructive, unlike the CI question. That asymmetry is
 * deliberate — see [removeBackend].
 */
fun wantsBackend(cliBackendEnabled: Boolean?): Boolean {
    if (cliBackendEnabled != null) return cliBackendEnabled

    println()
    printCyan("""
        🗄️  Does this project ship its own backend?

        The template includes a Ktor + Postgres server (:apps:server), the
        remote-config admin console it serves (:apps:admin), and the end-to-end
        harness that drives the client against it (:apps:integration) — plus
        the Fly.io deploy workflows and the CI jobs that test them.

        This is NOT asking whether your app uses the network. Supabase auth and
        every third-party HTTP call live in :libraries:*, and they stay either
        way. It is asking whether YOU are shipping a server from this repo.

        Say no and all of the above is deleted. Keep it and you can ignore it —
        the server boots in a limited mode with no config and costs nothing
        until you deploy it.

        Unlike the CI question, no is not reversible from inside this project:
        nothing is staged for a later opt-in, because switching a backend on is
        not a file move. Adding one later means taking it from the template.
    """.trimIndent())
    println()
    print("Include the backend? (Y/n): ")
    val answer = readln().trim().lowercase()
    return answer.isEmpty() || answer == "y" || answer == "yes"
}

/**
 * Substitute the {{APP_NAME}} / {{APP_SLUG}} / {{CONTACT_EMAIL}} /
 * {{LAST_UPDATED}} / {{APP_TAGLINE}} / {{APP_DESCRIPTION}} placeholders that
 * live inside the CI staging files. Runs after replaceFileContents so it
 * applies to the already-copied, already-renamed content.
 */
fun substitutePlaceholders(projectDir: File, projectName: ProjectName, contactEmail: String) {
    val today = java.time.LocalDate.now().toString()
    val tagline = "${projectName.displayName}, the official site."
    val description = "${projectName.displayName} is a cross-platform app built with Kotlin Multiplatform and Compose."
    val pairs = listOf(
        "{{APP_NAME}}" to projectName.displayName,
        // The first path segment of the published legal URLs, e.g.
        // nightjarlabs.llc/my-awesome-app/privacy. Kebab-case because it is a
        // URL. Once a store listing is live this is not a cosmetic string:
        // changing it means re-filing on both stores.
        "{{APP_SLUG}}" to projectName.kebabCase,
        "{{CONTACT_EMAIL}}" to contactEmail,
        "{{LAST_UPDATED}}" to today,
        "{{APP_TAGLINE}}" to tagline,
        "{{APP_DESCRIPTION}}" to description
    )

    fun walk(f: File) {
        if (f.isDirectory) {
            if (f.name in SKIP_DIRECTORIES) return
            f.listFiles()?.forEach(::walk)
            return
        }
        if (!shouldProcessFile(f)) return
        try {
            var content = f.readText()
            val original = content
            for ((k, v) in pairs) content = content.replace(k, v)
            if (content != original) f.writeText(content)
        } catch (_: Exception) {}
    }

    walk(projectDir)
}

fun cleanupTemplateArtifacts(projectDir: File, projectName: ProjectName, ciEnabled: Boolean) {
    val toDelete = mutableListOf(
        "scripts/init_project.main.kts",
        "scripts/rename_to_template.sh",
        // Template-maintenance tooling and planning docs — meaningful only in
        // the template repo itself, never in a generated project.
        "scripts/verify_template.sh",
        "docs/template-maintenance.md",
        "docs/cards-backport-plan.md",
        "docs/template-upgrade-execution-plan.md",
        // The port queue and its plans are the template's own backlog. They name
        // the source apps systems were carried back from, which a generated
        // project has no use for and the de-branding check would flag.
        "docs/PORT-CANDIDATES.md",
        "docs/plans"
    )
    if (ciEnabled) {
        // CI is already installed, so the late-opt-in path has nothing to do.
        toDelete += "scripts/enable_ci.sh"
    }
    toDelete.forEach { relative ->
        val file = File(projectDir, relative)
        if (file.exists()) {
            // deleteRecursively, not delete: the list holds directories as well
            // as files, and File.delete() is a silent no-op on a non-empty one.
            file.deleteRecursively()
            printGreen("   ✓ Removed $relative")
        }
    }

    rewriteReadme(projectDir, projectName)
    rewriteAgentsMd(projectDir, projectName)
}

fun rewriteReadme(projectDir: File, projectName: ProjectName) {
    val readmeFile = File(projectDir, "README.md")
    if (!readmeFile.exists()) return

    var content = readmeFile.readText()

    val initSectionHeader = "### Initialize Your Project"
    val nextSection = "### Build & Run"
    val startIdx = content.indexOf(initSectionHeader)
    val endIdx = content.indexOf(nextSection)
    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
        content = content.removeRange(startIdx, endIdx)
    }

    // The name variants have already been substituted by this point, so these
    // anchors are written post-rename. Matching "# KMP Template" here would
    // never fire.
    content = content
        .replace(
            "A Kotlin Multiplatform template with the production systems",
            "A Kotlin Multiplatform app with the production systems"
        )
        .replace("## Quick Start\n\n### Build & Run", "## Build & Run")
        // docs/PORT-CANDIDATES.md is the template's own backlog and is deleted
        // from generated projects, so both references to it are dead links.
        .replace(
            "| [docs/PORT-CANDIDATES.md](docs/PORT-CANDIDATES.md) | The queue of things downstream apps proved that belong here |\n",
            ""
        )
        .replace(
            "that goes in [docs/PORT-CANDIDATES.md](docs/PORT-CANDIDATES.md). Code,",
            "that goes in the template's own `docs/PORT-CANDIDATES.md`. Code,"
        )

    readmeFile.writeText(content)
    printGreen("   ✓ Rewrote README.md")
}

fun rewriteAgentsMd(projectDir: File, projectName: ProjectName) {
    val agentsFile = File(projectDir, "AGENTS.md")
    if (!agentsFile.exists()) return

    var content = agentsFile.readText()

    content = content
        .replace(
            "Guidelines for AI agents working in this KMP template repository.",
            "Guidelines for AI agents working in the ${projectName.displayName} repository."
        )
        .replace(
            "KMP (Kotlin Multiplatform) template with",
            "KMP (Kotlin Multiplatform) app with"
        )

    agentsFile.writeText(content)
    printGreen("   ✓ Rewrote AGENTS.md")
}

// ── backend removal ─────────────────────────────────────────────────────────

/**
 * Aborts the run with a message the template maintainer can act on.
 *
 * Every edit below is anchored on exact text. When an anchor goes missing it
 * means this repo changed and this script did not change with it, and the only
 * safe move is to stop: a silently-skipped edit ships a generated project that
 * still half-believes it has a server, which is precisely the confusion this
 * whole option exists to remove. `scripts/verify_template.sh` runs the
 * no-backend path, so drift is caught here rather than downstream.
 */
fun initFail(message: String): Nothing {
    printRed("❌ $message")
    throw IllegalStateException(message)
}

fun applyEdits(file: File, edits: List<Pair<String, String>>) {
    if (!file.exists()) initFail("Backend removal expected ${file.path} to exist")
    var content = file.readText()
    for ((old, new) in edits) {
        if (!content.contains(old)) {
            initFail(
                "Backend removal could not find this text in ${file.name}:\n" +
                    old.lineSequence().take(3).joinToString("\n") { "     $it" } + "\n" +
                    "   Update the anchor in init_project.main.kts to match."
            )
        }
        content = content.replace(old, new)
    }
    file.writeText(content)
}

/**
 * Locates exactly one file named [fileName] under [dir].
 *
 * Source files live under a package path this run has already rewritten, so
 * their full relative path is not knowable here. Failing on anything but a
 * single match keeps that from quietly editing the wrong copy.
 */
fun findOne(dir: File, fileName: String): File {
    val matches = dir.walkTopDown().filter { it.isFile && it.name == fileName }.toList()
    return when (matches.size) {
        1 -> matches.single()
        0 -> initFail("Backend removal found no $fileName under ${dir.path}")
        else -> initFail("Backend removal found ${matches.size} files named $fileName under ${dir.path}")
    }
}

/**
 * Removes the markdown section headed by [heading], stopping at the next
 * heading of the same level or higher, or at a horizontal rule.
 *
 * Structural rather than an exact-text anchor on the whole section. A section's
 * body gets reworded constantly and its heading almost never does, so quoting
 * forty lines verbatim would turn every doc edit into a broken generator — and
 * the failure would land on whoever did the rewording, who has no idea this
 * code exists.
 */
fun cutSection(file: File, heading: String) {
    if (!file.exists()) initFail("Backend removal expected ${file.path} to exist")
    val level = heading.takeWhile { it == '#' }.length
    val lines = file.readText().split("\n")
    val start = lines.indexOfFirst { it.trimEnd() == heading }
    if (start < 0) initFail("${file.name} has no section headed `$heading`")
    val boundary = Regex("^#{1,$level} ")
    val end = (start + 1 until lines.size)
        .firstOrNull { boundary.containsMatchIn(lines[it]) || lines[it].trimEnd() == "---" }
        ?: lines.size
    file.writeText((lines.subList(0, start) + lines.subList(end, lines.size)).joinToString("\n"))
}

/**
 * Removes every top-level markdown list item in [file] whose first line
 * [matches], along with its indented continuation lines.
 *
 * Same reasoning as [cutSection]: what a bullet is *about* is stable, its
 * wording is not. Fails when nothing matched, so a bullet that gets rewritten
 * past recognition is reported rather than silently left behind.
 */
fun dropListItems(file: File, description: String, matches: (String) -> Boolean) {
    if (!file.exists()) initFail("Backend removal expected ${file.path} to exist")
    val lines = file.readText().split("\n")
    val kept = mutableListOf<String>()
    var removed = 0
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        if (line.startsWith("- ") && matches(line)) {
            removed++
            index++
            while (index < lines.size && lines[index].isNotBlank() && lines[index].startsWith("  ")) index++
            continue
        }
        kept += line
        index++
    }
    if (removed == 0) initFail("$description: no matching list item in ${file.name}")
    file.writeText(kept.joinToString("\n"))
}

/** Removes every markdown table row in [file] that [matches]. */
fun dropTableRows(file: File, description: String, matches: (String) -> Boolean) {
    if (!file.exists()) initFail("Backend removal expected ${file.path} to exist")
    val lines = file.readText().split("\n")
    val kept = lines.filterNot { it.trimStart().startsWith("|") && matches(it) }
    if (kept.size == lines.size) initFail("$description: no matching table row in ${file.name}")
    file.writeText(kept.joinToString("\n"))
}

/** Net `{` minus `}` on a line, ignoring line comments and string literals. */
fun braceDelta(line: String): Int {
    var depth = 0
    var inString = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inString && c == '\\' -> i++
            c == '"' -> inString = !inString
            inString -> Unit
            c == '/' && i + 1 < line.length && line[i + 1] == '/' -> return depth
            c == '{' -> depth++
            c == '}' -> depth--
        }
        i++
    }
    return depth
}

/**
 * Deletes the `if (<guard>) {` line and its matching `}`, dedenting the body by
 * four spaces.
 *
 * Brace-counted rather than pattern-matched on the closing line. In
 * `settings.gradle.kts` the body of the `serverOnly` guard is `:apps:compose`,
 * every feature and every library, and its opening lines read like a small
 * self-contained section — so "delete the block" is the natural mistake, and it
 * leaves a settings file that still parses and includes almost nothing.
 */
fun unwrapGuard(lines: List<String>, guardLine: String, file: String): List<String> {
    val start = lines.indexOfFirst { it.trim() == guardLine }
    if (start < 0) initFail("$file no longer opens with `$guardLine`")
    var depth = 0
    var end = -1
    for (i in start until lines.size) {
        depth += braceDelta(lines[i])
        if (depth == 0) {
            end = i
            break
        }
    }
    if (end < 0) initFail("Unbalanced braces after `$guardLine` in $file")
    val body = lines.subList(start + 1, end).map { it.removePrefix("    ") }
    return lines.subList(0, start) + body + lines.subList(end + 1, lines.size)
}

/** Fails unless [text] contains every one of [required] and none of [forbidden]. */
fun assertContents(text: String, file: String, required: List<String>, forbidden: List<String>) {
    required.firstOrNull { it !in text }
        ?.let { initFail("Backend removal left $file without `$it` — it removed too much.") }
    forbidden.firstOrNull { it in text }
        ?.let { initFail("Backend removal left `$it` behind in $file.") }
    if (text.lines().sumOf { braceDelta(it) } != 0) {
        initFail("Backend removal unbalanced the braces in $file")
    }
}

fun collapseBlankRuns(lines: List<String>): List<String> {
    val out = mutableListOf<String>()
    for (line in lines) {
        if (line.isBlank() && out.lastOrNull()?.isBlank() == true) continue
        out += line
    }
    return out
}

fun dropInclude(lines: MutableList<String>, module: String, withLeadingComment: Boolean) {
    val index = lines.indexOfFirst { it.trim() == "include(\"$module\")" }
    if (index < 0) initFail("settings.gradle.kts no longer includes $module")
    lines.removeAt(index)
    if (!withLeadingComment) return
    var i = index - 1
    while (i >= 0 && lines[i].trimStart().startsWith("//")) {
        lines.removeAt(i)
        i--
    }
}

fun stripServerFromSettings(file: File) {
    var lines = unwrapGuard(file.readText().split("\n"), "if (!serverOnly) {", file.name)

    // The serverOnly property exists only to slim the build graph for the
    // server's Docker image. With no image it is dead weight that names a file
    // this run just deleted.
    val declarationStart = lines.indexOfFirst { it.startsWith("// `-DserverOnly=true`") }
    val declarationEnd = lines.indexOfFirst { it.startsWith("val serverOnly") }
    if (declarationStart < 0 || declarationEnd < declarationStart) {
        initFail("settings.gradle.kts no longer has the serverOnly comment + declaration")
    }
    lines = lines.subList(0, declarationStart) + lines.subList(declarationEnd + 1, lines.size)

    val mutable = lines.toMutableList()
    // `:apps:server` sits under a comment shared with `include(":apps")`, so
    // only its own line goes; the other two own the comment above them.
    dropInclude(mutable, ":apps:server", withLeadingComment = false)
    dropInclude(mutable, ":apps:integration", withLeadingComment = true)
    dropInclude(mutable, ":apps:admin", withLeadingComment = true)

    val text = collapseBlankRuns(mutable).joinToString("\n").trimEnd() + "\n"
    file.writeText(text)

    applyEdits(file, listOf(
        "// Apps (always included)" to "// Apps",
        """
        // com.android.test module that ships nothing and exists only at build time,
        // so it is client-side and gated out of the server-only graph.
        """.trimIndent() to
            "// com.android.test module that ships nothing and exists only at build time.",
        """
        // `detektPlugins`. Dev/CI tooling only, never shipped; gated out of the
        // server-only Docker build like every other client module.
        """.trimIndent() to "// `detektPlugins`. Dev/CI tooling only, never shipped.",
    ))

    assertContents(
        file.readText(), file.name,
        required = listOf("include(\":apps:compose\")", "include(\":libraries:ui\")", "include(\":detekt-rules\")"),
        forbidden = listOf("serverOnly", ":apps:server", ":apps:admin", ":apps:integration"),
    )
}

fun stripServerFromRootBuild(file: File) {
    applyEdits(file, listOf(
        """
        // source-scanning task (detekt's "plain" task) sidesteps per-module KMP source-set
        // wiring. Skipped in the server-only Docker build, where :detekt-rules isn't on the
        // build graph (it's a client/dev module) and linting isn't wanted anyway.
        """.trimIndent() to
            "// source-scanning task (detekt's \"plain\" task) sidesteps per-module KMP source-set\n// wiring.",
    ))
    val unwrapped = unwrapGuard(
        file.readText().split("\n"),
        "if (System.getProperty(\"serverOnly\") != \"true\") {",
        file.name,
    ).joinToString("\n").trimEnd() + "\n"
    file.writeText(unwrapped)
    assertContents(
        unwrapped, file.name,
        required = listOf("\"detektPlugins\"(project(\":detekt-rules\"))", "verifyComposeResourceStrings"),
        forbidden = listOf("serverOnly"),
    )
}

/**
 * Drops a top-level job from a GitHub Actions workflow.
 *
 * Job names are the only keys indented exactly two spaces (a job's own keys sit
 * at four), so the next such line is where the job ends.
 */
fun removeWorkflowJob(file: File, job: String) {
    val lines = file.readText().split("\n")
    val start = lines.indexOfFirst { it == "  $job:" }
    if (start < 0) initFail("${file.name} has no job named '$job'")
    val jobKey = Regex("^ {2}[A-Za-z0-9_-]+:\\s*$")
    val end = (start + 1 until lines.size).firstOrNull { jobKey.matches(lines[it]) } ?: lines.size
    val kept = lines.subList(0, start) + lines.subList(end, lines.size)
    file.writeText(kept.joinToString("\n").trimEnd() + "\n")
}

/**
 * Strips the backend from a generated project.
 *
 * Destructive on purpose, and unlike the CI opt-out nothing is staged for a
 * later change of mind. The CI question can stage `template/ci/` because
 * turning CI on afterwards is a pure file move. A backend is not: it has to
 * come back into `settings.gradle.kts`, into the CI workflow, and into two
 * deploy workflows, so a staged copy would be a folder of source with no
 * switch. Adding a backend later is a diff against the template, which is
 * where it stays maintained.
 */
fun removeBackend(projectDir: File, projectName: ProjectName) {
    printYellow("   → No backend: removing the server, admin console and integration harness.")

    listOf(
        // Carries fly.toml, fly.prod.toml, Dockerfile, docker-compose.yml,
        // DEPLOY.md, README.md and the staged admin-web bundle.
        "apps/server",
        // Served by the server at /admin and shipped inside its image, so it
        // cannot outlive it.
        "apps/admin",
        // apps/integration/build.gradle.kts declares implementation(projects.apps.server).
        "apps/integration",
        // Both when CI was installed, and in the staging folder that
        // scripts/enable_ci.sh would install later.
        ".github/workflows/server-deploy.yml",
        ".github/workflows/server-deploy-prod.yml",
        "template/ci/.github/workflows/server-deploy.yml",
        "template/ci/.github/workflows/server-deploy-prod.yml",
        // A plan for the thing that is no longer here.
        "docs/backend-and-supabase-auth-plan.md",
        // Provisions Fly apps for a server this project does not have. It
        // refuses to run without one, but a script that only ever declines is
        // still one more thing to read and wonder about.
        "scripts/setup_fly.main.kts",
    ).forEach { relative ->
        val target = File(projectDir, relative)
        if (target.exists()) {
            target.deleteRecursively()
            printGreen("   ✓ Removed $relative")
        }
    }

    stripServerFromSettings(File(projectDir, "settings.gradle.kts"))
    stripServerFromRootBuild(File(projectDir, "build.gradle.kts"))
    printGreen("   ✓ Trimmed the Gradle build graph")

    // Whichever copy of the CI workflow survived the CI question: installed
    // under .github/ when CI was accepted, still staged under template/ci/
    // when it was declined and scripts/enable_ci.sh would install it later.
    val workflows = listOf(".github/workflows/ci.yml", "template/ci/.github/workflows/ci.yml")
        .map { File(projectDir, it) }
        .filter { it.exists() }
    if (workflows.isEmpty()) initFail("Neither the installed nor the staged ci.yml is present")
    workflows.forEach { workflow ->
            applyEdits(workflow, listOf(
                """
                #               but not APKs. Each test job publishes its own JUnit XML as an
                #               inline check via dorny/test-reporter ("Unit test results",
                #               "Server test results", "Integration test results") with
                #               pass/fail counts + click-through to failures. Ship by
                """.trimIndent() to
                    """
                    #               but not APKs. The test job publishes its JUnit XML as an
                    #               inline check via dorny/test-reporter ("Unit test results")
                    #               with pass/fail counts + click-through to failures. Ship by
                    """.trimIndent(),
                // trimMargin, not trimIndent: these lines are YAML, so their
                // leading spaces are the anchor. trimIndent would strip the
                // shallowest line's indent off every line and match nothing.
                """
                |      - name: Run unit tests
                |        # The end-to-end :apps:integration tests run in their own job (cheaper
                |        # Ubuntu runner with Docker for testcontainers, no Xcode/iOS needed) so
                |        # a slower e2e can't blur the unit-test signal — exclude them from this
                |        # sweep to avoid running twice.
                |        run: ./gradlew testDebugUnitTest -x :apps:integration:testDebugUnitTest
                """.trimMargin() to
                    "      - name: Run unit tests\n        run: ./gradlew testDebugUnitTest",
            ))
            removeWorkflowJob(workflow, "server-test")
            removeWorkflowJob(workflow, "integration-test")
            printGreen("   ✓ Dropped the server + integration jobs from ${workflow.name}")
        }

    applyEdits(File(projectDir, ".gitignore"), listOf(
        """
        # Prebuilt admin console bundle staged for the server Docker image (CI-built)
        apps/server/admin-web/*
        !apps/server/admin-web/.gitkeep

        # Server runtime secrets. Real values live in apps/server/.env locally
        # and in Fly.io secrets in production. Only .env.example is committed.
        apps/server/.env
        apps/server/.env.local
        apps/server/.env.*.local

        """.trimIndent() + "\n" to "",
        """
        # Admin tool: local-only dev/prod admin tokens (baked into the bundle at build time)
        apps/admin/admin-tokens.local.properties

        """.trimIndent() + "\n" to "",
    ))

    stripServerFromSetupDoc(File(projectDir, "SETUP.md"))
    stripServerFromAgentsDoc(File(projectDir, "AGENTS.md"))
    stripServerFromReadme(File(projectDir, "README.md"))
    cutSection(File(projectDir, "scripts/README.md"), "## setup_fly.main.kts")
    applyEdits(File(projectDir, "scripts/README.md"), listOf(
        "Supabase half of `apps/server/.env`. Needs a Supabase personal access token" to
            "Supabase values for a server, if this project grows one. Needs a Supabase\npersonal access token",
    ))
    stripServerFromTestingDoc(File(projectDir, "docs/practices/testing.md"))
    stripServerFromObservabilityDoc(
        File(projectDir, "docs/practices/observability.md"),
        serviceName = "${projectName.lowercase}-server",
    )
    cutSection(File(projectDir, "libraries/config/README.md"), "## Server side")
    printGreen("   ✓ Removed the backend from the docs")

    stripServerFromSourceComments(projectDir)
    printGreen("   ✓ Removed the backend from code comments and detekt config")

    // The decision log is history, so entries naming a thing that no longer
    // exists are not wrong — but four of them are wholly about how to build a
    // server, which is architecture this project does not have and will not
    // gain by reading about. The 2026-09-20 entry explaining *why* there is no
    // server stays: for this reader it is the most useful one in the file.
    val decisions = File(projectDir, "docs/decisions.md")
    listOf(
        "## 2026-06-21: Server mirrors client conventions",
        "## 2026-06-21: Graceful degradation over required config",
        "## 2026-06-21: Auth is JWKS verification, never a shared secret",
        "## 2026-06-21: `serverOnly` build slimming",
        "## 2026-06-21: Flyway SQL is the schema source of truth",
    ).forEach { cutSection(decisions, it) }
    printGreen("   ✓ Removed the backend's architecture decisions")
}

/**
 * KDoc and build-file comments that cite a module this run just deleted, plus
 * the detekt exclude for the admin console.
 *
 * Nothing here is load-bearing — a stale comment compiles. It is in scope
 * because the cost of a comment naming a module that does not exist is paid by
 * whoever reads it: they go looking for `:apps:integration` and conclude the
 * checkout is broken rather than that the comment is.
 */
fun stripServerFromSourceComments(projectDir: File) {
    val networking = File(projectDir, "libraries/networking/impl/src/commonMain/kotlin")

    applyEdits(findOne(networking, "WebSocketKeepalive.kt"), listOf(
        " * Client→server WebSocket ping cadence. Matches the server's own 15s\n" +
            " * `pingPeriod` (apps/server .../plugins/WebSockets.kt) so either side notices\n" +
            " * a dead peer within a couple of cadences." to
            " * Client→server WebSocket ping cadence. Pick a value the server side\n" +
                " * matches, so either side notices a dead peer within a couple of cadences.",
        "\n *\n * Regression guard: `SocketKeepaliveTest` in `:apps:integration`." to "",
    ))

    applyEdits(findOne(networking, "WiretapNetworkInspector.kt"), listOf(
        " * that only runs in a real app process — NOT in host-JVM unit tests (e.g. the\n" +
            " * `:apps:integration` harness, which runs the real client as `testDebugUnitTest`\n" +
            " * with `isDebug == true`). Installing it there makes every request crash with" to
            " * that only runs in a real app process — NOT in host-JVM unit tests that\n" +
                " * drive the real client with `isDebug == true`. Installing it there makes\n" +
                " * every request crash with",
    ))

    applyEdits(findOne(networking, "NetworkClientImpl.kt"), listOf(
        " * Server's `403 AccessDeniedResponse` wire shape — duplicated here (not shared)\n" +
            " * because the server class lives in `:apps:server` which the client must not\n" +
            " * depend on. Locked machine-readable data only, no copy: the client localizes" to
            " * Server's `403 AccessDeniedResponse` wire shape, declared client-side because\n" +
                " * the client owns no part of the server's source. Locked machine-readable\n" +
                " * data only, no copy: the client localizes",
    ))

    applyEdits(File(projectDir, "libraries/telemetry/impl/build.gradle.kts"), listOf(
        "// Excluded here rather than globally: `:apps:server` uses CIO deliberately, on\n" +
            "// the JVM, where it has TLS." to
            "// Excluded from the client graph rather than globally: CIO is a reasonable\n" +
                "// choice on the JVM, where it has TLS.",
    ))

    applyEdits(File(projectDir, "gradle/libs.versions.toml"), listOf(
        "# JWKS (see :apps:server). Only the GoTrue/auth module is needed client-side." to
            "# Only the GoTrue/auth module is needed client-side.",
    ))

    applyEdits(File(projectDir, "config/detekt/detekt.yml"), listOf(
        "\n    # The config admin tool (:apps:admin) is a local-only, English-only dev\n" +
            "    # utility with no :libraries:resources dependency — the i18n rule doesn't\n" +
            "    # apply. Its `Text(...)` is Compose HTML's, not the DS composable anyway.\n" +
            "    excludes: ['**/apps/admin/**']" to "",
    ))
}

fun stripServerFromTestingDoc(file: File) {
    dropListItems(file, "server test layers") { line ->
        line.startsWith("- **Server unit + route tests**") ||
            line.startsWith("- **Full-stack server test**") ||
            line.startsWith("- **End-to-end integration**") ||
            line.startsWith("- **Integration tests belong in")
    }
    cutSection(file, "## The integration harness (`:apps:integration`)")
    dropTableRows(file, "server + integration bug classes") { row ->
        ":apps:server" in row || ":apps:integration" in row || "FullStackMeTest" in row
    }
    applyEdits(file, listOf(
        """

        Integration tests aren't a substitute for unit tests, they're slower and
        harder to debug. Use them for the seam contract (real wire, real plumbing),
        not for every rule the lower layers already own.
        """.trimIndent() + "\n" to "",
        " Integration tests use a **real** Main\n" +
            "  dispatcher (`Dispatchers.setMain(Dispatchers.Default)`) because real sockets\n" +
            "  run on real threads." to "",
    ))
}

/**
 * @param serviceName the Loki `service_name` of the server this project will
 *   not have. Passed in rather than hard-coded: this runs after the rename
 *   pass, so the doc's copy already carries the new project's name.
 */
fun stripServerFromObservabilityDoc(file: File, serviceName: String) {
    dropListItems(file, "server telemetry legs") { it.startsWith("- **Server → ") }
    applyEdits(file, listOf(
        "# Observability: one id, three systems" to "# Observability: one id, two systems",
        """
        The stack is Sentry (crashes, user feedback, stack traces), Loki (logs: client app events and
        server request logs), and Tempo (server traces). What ties them together is a single correlation
        id: **`session_id`**, the UUID of the current client app session.
        """.trimIndent() to
            """
            The stack is Sentry (crashes, user feedback, stack traces) and Loki (client app events and
            Warn+ logs). What ties them together is a single correlation id: **`session_id`**, the UUID
            of the current client app session.
            """.trimIndent(),
        "- **Client → server.** `DefaultClientHeadersProvider` sends it on every request as `X-Session-Id`\n" +
            "  (plus `X-Install-Id`)." to
            "- **Client → whatever you call.** `DefaultClientHeadersProvider` sends it on every request as\n" +
                "  `X-Session-Id` (plus `X-Install-Id`), so a backend that reads the header can correlate too.",
        """
        The key naming rule: it is always the underscore form `session_id`, in all systems, so one query
        string works everywhere. The same rule applies to any context you add: if a key exists on backend
        spans and client Sentry tags, spell it identically (`Telemetry.setContext(key, value)` client-side,
        `SpanAttrs` server-side).
        """.trimIndent() to
            """
            The key naming rule: it is always the underscore form `session_id`, in all systems, so one query
            string works everywhere. The same rule applies to any context you add — spell a key identically
            wherever it appears (`Telemetry.setContext(key, value)` client-side).
            """.trimIndent(),
        "\n\n# Server logs for one session\n{service_name=\"$serviceName\"} | session_id=\"<uuid>\"" to "",
        """
        3. **Backend side:** `{service_name="$serviceName"} | session_id="<uuid>"` for logs;
           `{ .session_id = "<uuid>" }` in Tempo for every request trace the session produced.
        4. **The reverse direction works too:** a server error in Sentry carries `trace_id` (paste into
           Tempo) and `session_id` (pull the client's events), so backend-first investigations reach the
           client story in one hop.
        """.trimIndent() + "\n" to "",
        " The server's OTel pipeline is gated by a\n" +
            "single env var: `OTEL_EXPORTER_OTLP_ENDPOINT` unset → stdout exporters, set → OTLP/HTTP." to "",
    ))
}

fun stripServerFromSetupDoc(file: File) {
    cutSection(file, "### Server deploy (Fly.io)")

    applyEdits(file, listOf(
        "- [ ] [Server deploy](#server-deploy-flyio): dev Fly app + secrets + `/_health`\n" to "",
        """
        5. Server env (see `apps/server/.env.example`): `SUPABASE_URL` for JWT
           verification, and `SUPABASE_SERVICE_ROLE_KEY` if you want in-app account
           deletion (`DELETE /v1/me`) and display-name mirroring. Treat the service
           role key as a root password, server secrets only, never the client.
        6. Verify: launch the app → complete onboarding as a guest → a user appears
           in Supabase → Authentication → Users with `is_anonymous = true`, and
           `GET /v1/me` (through the app) creates the profile row.
        """.trimIndent() to
            """
            5. Verify: launch the app → complete onboarding as a guest → a user
               appears in Supabase → Authentication → Users with
               `is_anonymous = true`.
            """.trimIndent(),
        // The Day-1 list is rewritten whole rather than having two items cut out
        // of it: the surviving checks are renumbered, and check 3 asks you to
        // match a client session id against server request logs.
        """
        1. **Server is live.**
           ```sh
           curl https://<your-app>-server-dev.fly.dev/_health
           ```
           Expected: `{"ok":true}`.
        2. **Client → server round trip.** Launch the app (device or simulator),
           complete onboarding as a guest. Expected: a new user in Supabase →
           Authentication → Users with `is_anonymous = true`, and a row in the
           `profiles` table.
        3. **Find your session in Loki** (if Grafana is wired). In Grafana → Explore →
           Loki, query your client logs by the app's service name and filter
           `session_id="<id>"`, grab the id from the app's debug shake dialog or
           logcat (`Session started`). Expected: the `app.launched` event and your
           request logs, and the SAME `session_id` on the server's request logs.
        4. **Trigger a test crash → Sentry.** Requires
        """.trimIndent() to
            """
            1. **Auth works.** Launch the app (device or simulator), complete
               onboarding as a guest. Expected: a new user in Supabase →
               Authentication → Users with `is_anonymous = true`.
            2. **Find your session in Loki** (if Grafana is wired). In Grafana →
               Explore → Loki, query your client logs by the app's service name and
               filter `session_id="<id>"` — grab the id from the app's debug shake
               dialog or logcat (`Session started`). Expected: the `app.launched`
               event and your request logs.
            3. **Trigger a test crash → Sentry.** Requires
            """.trimIndent(),
        """
        5. **Config round trip.** Open the admin console (`/admin` on the dev
           server, paste your `ADMIN_API_TOKEN`), flip `upgrade.maintenanceMessage`
           to a test string, foreground the app twice (refresh is throttled).
           Expected: the value changes in the QA config dashboard; the audit tab
           records your change.
        """.trimIndent() to "",
        "Prove the observability + deploy story end to end while everything is fresh." to
            "Prove the observability story end to end while everything is fresh.",
        "Sentry org + tokens, Supabase + Fly tokens and org slugs, Apple team" to
            "Sentry org + tokens, Supabase token and org slug, Apple team",
        "belongs to your Sentry org, your Fly\naccount or your Apple team, and is" to
            "belongs to your Sentry org or your\nApple team, and is",
        "derives from the `applicationId`, the Fly app name from the project name |" to
            "derives from the `applicationId` |",
        "Sentry project + DSN + CI vars, Fly apps + secrets + deploy tokens, every signing secret |" to
            "Sentry project + DSN + CI vars, every signing secret |",
        "| **Once per machine** | `gh auth login`, `fly auth login`, and filling" to
            "| **Once per machine** | `gh auth login`, and filling",
    ))
}

fun stripServerFromAgentsDoc(file: File) {
    cutSection(file, "## Server (`:apps:server`)")

    applyEdits(file, listOf(
        """
        Conventions (hand-rolled fakes only, dispatcher choice, which layer catches which bug) live in [`docs/practices/testing.md`](docs/practices/testing.md). Read it before adding tests. The end-to-end tier is `:apps:integration`: an Android-library module whose tests run on the host JVM (`./gradlew :apps:integration:testDebugUnitTest`, needs Docker) and drive the real client stack (real `HomeViewModel`, real repositories, real HTTP client) over real TCP against a real in-process Ktor server on a Testcontainers Postgres. `HarnessSmokeTest` is the worked example; `commonMain` stays empty so iOS never links the JVM-only server.
        """.trimIndent() to
            "Conventions (hand-rolled fakes only, dispatcher choice, which layer catches which bug) live in [`docs/practices/testing.md`](docs/practices/testing.md). Read it before adding tests.",
        "\n- **On the server, `withSpan` parents to the *current* OTel context.** Correct inside a request handler, wrong anywhere the current context outlives the unit of work. A WebSocket upgrade span stays current for the life of the socket, and a shared `Dispatchers.Default` scope leaves contexts on pool threads for unrelated work to inherit. Downstream this produced one trace id spanning hours and several users, permanently stuck at \"root span not yet received\". Root a new trace per unit of work. Full detail in the `withSpan` KDoc in `apps/server/.../plugins/Tracing.kt`." to "",
    ))
}

fun stripServerFromReadme(file: File) {
    applyEdits(file, listOf(
        "(Compose Multiplatform client, Ktor server on Fly.io, Supabase auth)" to
            "(Compose Multiplatform client, Supabase auth)",
        ", and a hosted **admin console** (Kotlin/JS) with targeting rules, audit log, and prod confirm-by-typing" to "",
        """

        **Server (Ktor + Postgres, deploys to Fly.io)**
        - Supabase JWT verification, ban gate (403 envelope the client understands), player reports (Google Play UGC compliance), account deletion (`DELETE /v1/me`), remote-config source + admin API, session-correlated tracing/logging
        - Boots gracefully with zero config (limited mode) and ships a docker-compose local stack
        - Two environments: dev auto-deploys on merge, prod behind an approval gate
        """.trimIndent() + "\n" to "",
        "- CI from the first push: build + unit/server/integration test jobs, release-please versioning" to
            "- CI from the first push: build + unit test jobs, release-please versioning",
        "- An **integration harness** that drives the real client stack against the real server over a real Postgres, in a unit test\n" to "",
        """

        # Server (boots in limited mode with zero config)
        ./gradlew :apps:server:run

        # Server with a local Postgres
        docker compose -f apps/server/docker-compose.yml up -d

        # Everything the CI gate runs
        ./gradlew testDebugUnitTest :apps:server:test :apps:integration:testDebugUnitTest
        """.trimIndent() to
            "\n# Everything the CI gate runs\n./gradlew testDebugUnitTest",
        """
        apps/server/           # Ktor + Postgres backend (Fly.io)
        apps/admin/            # Kotlin/JS remote-config admin console
        apps/integration/      # End-to-end harness (real client ↔ real server ↔ real DB)
        """.trimIndent() + "\n" to "",
        "| [docs/practices/testing.md](docs/practices/testing.md) | Which layer catches which bug; fakes; the integration harness |" to
            "| [docs/practices/testing.md](docs/practices/testing.md) | Which layer catches which bug; fakes |",
        "| [apps/server/DEPLOY.md](apps/server/DEPLOY.md) | Fly.io two-environment deployment |\n" to "",
        "| [apps/admin/README.md](apps/admin/README.md) | The remote-config admin console |\n" to "",
        "Supabase project + auth providers, Fly dev/prod apps, GitHub secrets" to
            "Supabase project + auth providers, GitHub secrets",
    ))
}

fun resetGitHistory(rootDir: File, projectName: ProjectName) {
    val gitDir = File(rootDir, ".git")
    if (gitDir.exists()) {
        gitDir.deleteRecursively()
        printGreen("   ✓ Removed old git history")
    }

    // DISCARD the child's output instead of leaving the default pipe: nothing
    // reads that pipe, and the initial commit of a full project prints enough
    // (one line per file) to fill the 64KB buffer — the child then blocks
    // writing and waitFor() deadlocks. Found the hard way via a hung smoke run.
    fun git(vararg args: String): Int = ProcessBuilder("git", *args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .start()
        .waitFor()

    val result = git("init")

    if (result == 0) {
        printGreen("   ✓ Initialized fresh git repository")

        git("add", ".")

        // Explicit identity + no signing so the commit succeeds on machines
        // with no global git identity (CI runners) and never blocks on a
        // host's signing agent.
        val commit = git(
            "-c", "user.name=Template Init",
            "-c", "user.email=init@localhost",
            "-c", "commit.gpgsign=false",
            "commit", "-m", "Initial commit - ${projectName.displayName}",
        )

        if (commit == 0) {
            printGreen("   ✓ Created initial commit")
        } else {
            printYellow("   ⚠ git commit failed (exit $commit) — commit manually after init")
        }
    } else {
        printYellow("   ⚠ Could not initialize git (git may not be installed)")
    }
}

/**
 * The closing summary: what is done, what is one command away, and what is
 * permanently yours.
 *
 * Written as three groups rather than one list because they are not the same
 * kind of work. Mixing "run this script" with "fill in Play Console's data
 * safety form" is how a checklist becomes something people stop reading — and
 * the cost of not reading it is a project that looks finished and silently is
 * not, which this template has shipped before.
 */
fun printNextSteps(projectDir: File, projectName: ProjectName, backendEnabled: Boolean) {
    println()
    printCyan("── Next ──")
    println()
    println("  cd ${projectDir.absolutePath}")
    println("  ./scripts/setup.main.kts        # walks the steps below, skipping what's done")
    println()
    printCyan("Automatic — each is also its own script, runnable any time:")
    println("  • Credentials   remembers what is identical across your projects,")
    println("                  so the next one stops asking")
    println("  • Supabase      creates the project, anonymous sign-ins on, redirect URLs,")
    println("                  Apple authorized for your bundle id")
    println("  • Sentry        creates the project, commits the DSN, proves an event lands")
    if (backendEnabled) {
        println("  • Fly.io        creates the dev + prod apps, pushes their secrets,")
        println("                  mints the CI deploy tokens")
    }
    println("  • Release       pushes every signing secret from one folder")
    println()
    printCyan("Yours, once per machine:")
    println("  • ./scripts/install_hooks.sh    (the build fails until you do)")
    println("  • gh auth login, and fly auth login if you have a server")
    println()
    printCyan("Yours, permanently — these have no API:")
    println("  • Play Console: the app entry, the service-account invite,")
    println("    data safety, content rating")
    println("  • App Store Connect: the app record, listing copy, screenshots")
    println("  • Promoting the first release by hand in each store, once")
    println()
    println("  SETUP.md is the full checklist. ./scripts/setup.main.kts --status")
    println("  answers 'where am I' at any point.")
    println()
    printGreen("🎉 Happy coding with ${projectName.displayName}!")
}

/**
 * Offers to run the setup chain right now.
 *
 * Declining costs nothing — `setup.main.kts` is re-runnable and every step
 * underneath is an upsert. Skipped entirely for `--yes` runs: automation asked
 * for a generated project, not for an interactive walkthrough it cannot answer.
 */
fun maybeRunSetup(projectDir: File, nonInteractive: Boolean) {
    if (nonInteractive) return
    println()
    print("Run ./scripts/setup.main.kts now? (Y/n): ")
    val answer = readln().trim().lowercase()
    if (answer.isNotEmpty() && answer != "y" && answer != "yes") {
        printYellow("→ Run it whenever you like. It picks up wherever you left off.")
        return
    }
    if (!File(projectDir, "scripts/setup.main.kts").exists()) {
        printYellow("→ scripts/setup.main.kts is missing — run the individual scripts instead.")
        return
    }
    println()
    // inheritIO: the steps prompt, and several of them hide secret input, which
    // only works against a real terminal. Failure is not fatal — the project is
    // already created, and the script can be re-run.
    val ran = try {
        ProcessBuilder("kotlin", "scripts/setup.main.kts")
            .directory(projectDir)
            .inheritIO()
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }
    if (!ran) {
        printYellow("→ Setup did not finish. Run ./scripts/setup.main.kts in the project when ready.")
    }
}

/** The template checkout this script is running from. */
fun templateRoot(): File = File(".").canonicalFile

/**
 * Resolves [input] to an absolute destination, treating a relative path as
 * relative to the template's **parent** rather than to the process's working
 * directory.
 *
 * The prompt suggests an absolute path beside the template, so answering it
 * with a bare `MyApp` plainly means "there". The JVM disagrees: `File("MyApp")`
 * resolves against `user.dir`, which is the template checkout itself, so the
 * new project silently lands *inside* the template. That has happened. The
 * nested project then shows up in the template's `git status` forever, and it
 * breaks `verify_template.sh` — init copies the whole tree, so the nested
 * project's own docs get copied into the smoke-test project and trip its
 * de-branding grep, with a failure that names files nobody recognises.
 */
fun resolveDestination(input: String): File {
    val base = templateRoot().parentFile ?: File(System.getProperty("user.home"))
    val raw = File(input)
    return if (raw.isAbsolute) raw.canonicalFile else File(base, input).canonicalFile
}

/** Why [dir] is not a legal destination, or null when it is fine. */
fun destinationProblem(dir: File): String? {
    val template = templateRoot()
    val inside = dir.canonicalPath.startsWith(template.canonicalPath + File.separator)
    return when {
        dir.canonicalPath == template.canonicalPath -> "that is the template itself"
        inside -> "it is inside the template checkout (${template.absolutePath})"
        else -> null
    }
}

fun getProjectDir(projectName: ProjectName): File? {
    val templateDir = templateRoot()
    val parentDir = templateDir.parentFile?.absolutePath ?: System.getProperty("user.home")
    val suggestedPath = File(parentDir, projectName.pascalCase).absolutePath

    println()
    printCyan("""
        📂 Where should the new project be created?

        This is the full path to the new project folder. It will be created
        if it does not already exist.

        Press Enter to use suggested: $suggestedPath
    """.trimIndent())
    println()
    print("Project directory [$suggestedPath]: ")

    val input = readln().trim()

    if (input.lowercase() in listOf("q", "quit", "exit")) {
        printYellow("👋 Goodbye!")
        return null
    }

    // Relative answers resolve against the template's parent, not the working
    // directory — see [resolveDestination]. "MyApp" means ../MyApp, which is
    // what the suggested path above implies.
    val projectDir = resolveDestination(input.ifEmpty { suggestedPath })

    destinationProblem(projectDir)?.let { problem ->
        printRed("❌ Cannot create the project there: $problem.")
        printYellow("   A generated project must live outside the template, or it")
        printYellow("   ends up tracked by the template's git and breaks its smoke test.")
        printYellow("   Suggested: $suggestedPath")
        return null
    }

    if (projectDir.exists() && projectDir.listFiles()?.isNotEmpty() == true) {
        printRed("❌ Directory already exists and is not empty: ${projectDir.absolutePath}")
        printYellow("   Choose a different location or remove the existing directory.")
        return null
    }

    if (!projectDir.exists() && !projectDir.mkdirs()) {
        printRed("❌ Could not create directory: ${projectDir.absolutePath}")
        return null
    }

    return projectDir
}

fun getProjectName(): ProjectName? {
    printCyan("""
        📛 Enter your project name
        
        This will be used to generate all naming variants:
        - Display name (e.g., "My Awesome App")
        - Code identifiers (e.g., MyAwesomeApp, myAwesomeApp)
        - File/folder names (e.g., my-awesome-app)
        
        Examples: "My App", "Super Todo", "Fitness Tracker"
    """.trimIndent())
    println()
    print("Project name: ")
    
    val input = readln().trim()
    
    if (input.isEmpty() || input.lowercase() in listOf("q", "quit", "exit")) {
        printYellow("👋 Goodbye!")
        return null
    }
    
    val projectName = validateProjectName(input)
    if (projectName == null) {
        printRed("❌ Invalid project name. Must start with a letter and use only letters, numbers, and spaces.")
        return null
    }
    return projectName
}

fun getContactEmail(): String? {
    println()
    printCyan("""
        ✉️ Contact email

        Shown on the published privacy policy and terms, and used as the
        default support address. You can change it later by editing the
        frontmatter of legal/*.md.

        Press Enter to use a placeholder (you@example.com).
    """.trimIndent())
    println()
    print("Contact email [you@example.com]: ")

    val input = readln().trim()
    if (input.lowercase() in listOf("q", "quit", "exit")) {
        printYellow("👋 Goodbye!")
        return null
    }
    return input.ifEmpty { "you@example.com" }
}

fun getPackageName(projectName: ProjectName): String? {
    val suggestedPackage = "com.example.${projectName.lowercase}"
    
    println()
    printCyan("""
        📦 Enter your package name
        
        This will be used for Kotlin/Java package declarations and Android namespace.
        Format: com.yourcompany.${projectName.lowercase}
        
        Press Enter to use suggested: $suggestedPackage
    """.trimIndent())
    println()
    print("Package name [$suggestedPackage]: ")
    
    val input = readln().trim()
    
    if (input.lowercase() in listOf("q", "quit", "exit")) {
        printYellow("👋 Goodbye!")
        return null
    }
    
    val packageName = validatePackageName(input.ifEmpty { suggestedPackage })
    if (packageName == null) {
        printRed("❌ Invalid package name. Must be lowercase, dot-separated, and start with a letter.")
        printRed("   Example: com.mycompany.myapp")
        return null
    }
    return packageName
}

fun buildReplacements(projectName: ProjectName, packageName: String): List<Pair<String, String>> {
    return listOf(
        // Package replacements (most specific first)
        TEMPLATE_PACKAGE to packageName,
        // The same package in path form. Docs and KDoc cite source files by
        // path (`libraries/core/src/commonMain/kotlin/com/kmptemplate/…`), and
        // the dotted rule above does not match a slashed path — so without
        // this the generic name rule below rewrites `com/kmptemplate/` to
        // `com/<projectname>/` while renamePackageDirectories moves the
        // directory to `com/<your/package>/`. Every such link then points at
        // nothing, in every generated project. Nothing but a link checker
        // catches it, because the path still looks plausible.
        TEMPLATE_PACKAGE.replace('.', '/') to packageName.replace('.', '/'),

        // Specific template-framing phrases (before generic name replacements)
        "Guidelines for AI agents working in this KMP template repository." to
                "Guidelines for AI agents working in the ${projectName.displayName} repository.",
        "KMP (Kotlin Multiplatform) template with" to "KMP (Kotlin Multiplatform) app with",
        "this KMP template repository" to "this ${projectName.displayName} repository",

        // Name replacements in various formats
        // SCREAMING case first (env vars like KMPTEMPLATE_WIRETAP_IOS — the
        // wiretap noop switch read by libraries/networking/impl, the staged
        // beta/release workflows, and the staged Fastfile must all agree
        // post-rename).
        TEMPLATE_NAME.lowercase.uppercase() to projectName.snakeCase.uppercase(),
        TEMPLATE_NAME.pascalCase to projectName.pascalCase,
        TEMPLATE_NAME.camelCase to projectName.camelCase,
        TEMPLATE_NAME.kebabCase to projectName.kebabCase,
        TEMPLATE_NAME.snakeCase to projectName.snakeCase,
        TEMPLATE_NAME.dotCase to projectName.dotCase,
        TEMPLATE_NAME.lowercase to projectName.lowercase,
        TEMPLATE_NAME.displayName to projectName.displayName,
        
        // Also handle "Kmp Template" and "kmp template" variations
        "Kmp Template" to projectName.displayName,
        "kmp template" to projectName.displayName.lowercase(),
        "KmpTemplate" to projectName.pascalCase,
        "Kmptemplate" to projectName.pascalCase,
        "kmptemplate" to projectName.lowercase
    )
}

fun replaceFileContents(dir: File, projectName: ProjectName, packageName: String, stats: ReplacementStats) {
    dir.listFiles()?.forEach { file ->
        if (file.name in SKIP_FILES) return@forEach
        
        if (file.isDirectory) {
            if (file.name !in SKIP_DIRECTORIES) {
                replaceFileContents(file, projectName, packageName, stats)
            }
        } else if (shouldProcessFile(file)) {
            val modified = replaceInFile(file, projectName, packageName, stats)
            if (modified) {
                stats.filesModified++
            }
        }
    }
}

fun shouldProcessFile(file: File): Boolean {
    val extension = file.extension.lowercase()
    return extension in TEXT_FILE_EXTENSIONS || file.name in listOf(
        // fastlane's files are extensionless — missing Appfile here shipped a
        // project whose TestFlight uploads looked up com.kmptemplate.KMPTemplate
        // on ASC and failed (found via Cards, 2026-07-09).
        "Podfile", "Gemfile", "Makefile", "Dockerfile", "gradlew",
        "Appfile", "Fastfile", "Matchfile", "Deliverfile",
        // .env files resolve to extension "example"/"env" — the server's
        // .env.example carries the project name in OTEL_SERVICE_NAME.
        ".env", ".env.example",
        // detekt's ServiceLoader registration: the file's dotted name defeats
        // extension matching, but its one line is the rule-set provider's FQN
        // under com.kmptemplate — without a rename the generated project's
        // detekt jar points at a class that no longer exists.
        "dev.detekt.api.RuleSetProvider",
        // Git hooks are extensionless; pre-push names the custom detekt
        // ruleset (`kmptemplate`) in its comments.
        "commit-msg", "post-commit", "pre-push",
    )
}

fun replaceInFile(file: File, projectName: ProjectName, packageName: String, stats: ReplacementStats): Boolean {
    try {
        var content = file.readText()
        val originalContent = content
        
        // Build replacement pairs in order of specificity (longer matches first)
        val replacements = buildReplacements(projectName, packageName)
        
        for ((old, new) in replacements) {
            val count = content.split(old).size - 1
            if (count > 0) {
                stats.replacementsMade += count
                content = content.replace(old, new)
            }
        }
        
        if (content != originalContent) {
            file.writeText(content)
            return true
        }
    } catch (e: Exception) {
        // Skip binary files or files we can't read
    }
    return false
}

fun renameDirectories(dir: File, projectName: ProjectName, packageName: String, stats: ReplacementStats) {
    // Package directories FIRST: com/kmptemplate must become the full package
    // path (com/your/pkg) before the generic name pass below renames it to
    // com/<lowercase> and leaves directories out of sync with the package
    // declarations rewritten by replaceFileContents (found via Cards, 2026-07-09:
    // dirs were com/cards while packages were com.dangerfield.cards).
    renamePackageDirectories(dir, packageName, stats)

    // Collect all directories first, then sort by depth (deepest first)
    val allDirs = mutableListOf<File>()
    collectDirectories(dir, allDirs)
    
    // Sort by path length descending (deepest paths first)
    allDirs.sortByDescending { it.absolutePath.length }
    
    for (directory in allDirs) {
        val newName = getReplacedName(directory.name, projectName)
        if (newName != directory.name) {
            val newDir = File(directory.parentFile, newName)
            if (directory.renameTo(newDir)) {
                stats.foldersRenamed++
            }
        }
    }
    
}

fun collectDirectories(dir: File, collected: MutableList<File>) {
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory && file.name !in SKIP_DIRECTORIES) {
            collected.add(file)
            collectDirectories(file, collected)
        }
    }
}

fun renamePackageDirectories(rootDir: File, newPackage: String, stats: ReplacementStats) {
    // Find and rename kmptemplate package directories to new package structure
    val oldPackagePath = TEMPLATE_PACKAGE.replace(".", File.separator)
    val newPackagePath = newPackage.replace(".", File.separator)
    
    fun findAndRenamePackageDirs(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name !in SKIP_DIRECTORIES) {
                val relativePath = file.absolutePath
                if (relativePath.contains(oldPackagePath)) {
                    val newPath = relativePath.replace(oldPackagePath, newPackagePath)
                    val newFile = File(newPath)
                    newFile.parentFile?.mkdirs()
                    if (file.renameTo(newFile)) {
                        stats.foldersRenamed++
                    }
                } else {
                    findAndRenamePackageDirs(file)
                }
            }
        }
    }
    
    findAndRenamePackageDirs(rootDir)
}

fun renameFiles(dir: File, projectName: ProjectName, stats: ReplacementStats) {
    dir.listFiles()?.forEach { file ->
        if (file.name in SKIP_FILES) return@forEach
        
        if (file.isDirectory) {
            if (file.name !in SKIP_DIRECTORIES) {
                renameFiles(file, projectName, stats)
            }
        } else {
            val newName = getReplacedName(file.name, projectName)
            if (newName != file.name) {
                val newFile = File(file.parentFile, newName)
                if (file.renameTo(newFile)) {
                    stats.filesRenamed++
                }
            }
        }
    }
}

fun getReplacedName(name: String, projectName: ProjectName): String {
    var result = name
    
    // Replace in order of specificity
    result = result.replace(TEMPLATE_NAME.pascalCase, projectName.pascalCase)
    result = result.replace(TEMPLATE_NAME.camelCase, projectName.camelCase)
    result = result.replace(TEMPLATE_NAME.kebabCase, projectName.kebabCase)
    result = result.replace(TEMPLATE_NAME.snakeCase, projectName.snakeCase)
    result = result.replace(TEMPLATE_NAME.lowercase, projectName.lowercase)
    result = result.replace("KmpTemplate", projectName.pascalCase)
    result = result.replace("Kmptemplate", projectName.pascalCase)
    result = result.replace("kmptemplate", projectName.lowercase)
    
    return result
}

// Run the script. Explicit exit: the script JVM has been observed lingering
// after main() completes (a stray non-daemon thread keeps it alive), which
// hangs automation that waits on the process.
main(parseCliConfig(args.toList()))
exitProcess(0)
