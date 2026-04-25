#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import java.io.File

/**
 * KMP Template Project Initialization Script
 * 
 * This script helps you set up a new project from the KMP Template.
 * It will rename all template placeholders to your chosen project name.
 * 
 * Usage: ./init_project.main.kts
 * 
 * The script will prompt you for:
 * - App name (e.g., "My Awesome App") - used for display
 * - Project identifier (e.g., "myawesomeapp") - used for module/package naming
 * - Package name (e.g., "com.example.myawesomeapp") - used for package declarations
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
    "xcconfig", "pbxproj", "xcscheme", "storyboard", "xib"
)

// Directories to skip during processing AND during copy
val SKIP_DIRECTORIES = setOf(
    ".git", ".gradle", ".idea", "build", "node_modules", ".kotlin",
    "caches", "generated", "intermediates"
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

fun main() {
    printBlue("""
        ╔══════════════════════════════════════════════════════════════╗
        ║         🚀 KMP Template Project Initialization 🚀            ║
        ╠══════════════════════════════════════════════════════════════╣
        ║  Creates a fresh copy of the template with your project     ║
        ║  name — the original template is left untouched.            ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent())
    println()

    val projectName = getProjectName() ?: return
    val packageName = getPackageName(projectName) ?: return
    val contactEmail = getContactEmail() ?: return
    val projectDir = getProjectDir(projectName) ?: return

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

    print("Proceed with these settings? (Y/n): ")
    val confirm = readln().trim().lowercase()
    if (confirm.isNotEmpty() && confirm != "y" && confirm != "yes") {
        printYellow("👋 Initialization cancelled. Run again when ready!")
        return
    }

    println()
    printBlue("🔄 Starting project initialization...")

    val stats = ReplacementStats()
    val templateDir = File(".").canonicalFile

    try {
        printBlue("📋 Step 1/8: Copying template to ${projectDir.absolutePath}...")
        copyTemplate(templateDir, projectDir)
        printGreen("   ✓ Template copied")

        printBlue("📦 Step 2/8: Placing SETUP.md + asking about CI...")
        placeSetupDoc(templateDir, projectDir)
        val ciEnabled = maybeEnableCi(templateDir, projectDir)

        printBlue("📝 Step 3/8: Replacing file contents...")
        replaceFileContents(projectDir, projectName, packageName, stats)

        printBlue("📁 Step 4/8: Renaming directories...")
        renameDirectories(projectDir, projectName, packageName, stats)

        printBlue("📄 Step 5/8: Renaming files...")
        renameFiles(projectDir, projectName, stats)

        printBlue("🔖 Step 6/8: Substituting CI placeholders...")
        substitutePlaceholders(projectDir, projectName, contactEmail)

        printBlue("🧹 Step 7/8: Cleaning up template artifacts...")
        cleanupTemplateArtifacts(projectDir, projectName)
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
        println()
        printYellow("✅ Project initialized.")
        printYellow("→ Open SETUP.md for the full action-item checklist")
        printYellow("   (GH secrets, store setup, first-release notes).")
        printYellow("→ Run ./scripts/install_hooks.sh before your first commit.")
        println()
        println("   Open the project:   cd ${projectDir.absolutePath}")
        println("   Sync Gradle:        open in IDE")
        println("   First build:        ./gradlew build")
        println()
        printGreen("🎉 Happy coding with ${projectName.displayName}!")

    } catch (e: Exception) {
        printRed("❌ Error during initialization: ${e.message}")
        e.printStackTrace()
        printYellow("⚠️  Partially created project may exist at: ${projectDir.absolutePath}")
        printYellow("     The original template was not modified.")
    }
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
        val target = File(dest, file.name)
        if (file.isDirectory) {
            copyTemplate(file, target)
        } else {
            file.copyTo(target, overwrite = false)
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
 * Returns the template staging directory inside the project dir, if it exists
 * (it gets copied there because its parent is the template root).
 *
 * Actually no — copyTemplate skips the template/ folder. So the staging source
 * is the original template's `template/` folder. We read it from the template
 * dir directly.
 */
/**
 * File.copyTo doesn't preserve the executable bit, so any shell scripts and
 * git hooks shipped in the template need +x applied explicitly after copy.
 */
fun ensureExecutableBits(projectDir: File) {
    val execPaths = listOf(
        "scripts/install_hooks.sh",
        ".githooks/commit-msg",
        ".githooks/post-commit",
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
 * Asks whether to enable CI and, if yes, copies every file under
 * template/ci/ into the project, preserving relative paths.
 */
fun maybeEnableCi(templateDir: File, projectDir: File): Boolean {
    val ciSrc = File(templateDir, "$TEMPLATE_STAGING_DIR/ci")
    if (!ciSrc.exists() || !ciSrc.isDirectory) return false

    println()
    printCyan("""
        🚢 Enable CI / release automation?

        This copies release-please, fastlane, GitHub Pages, and the Sentry
        triage prompt into your project:

          • .github/workflows/*.yml  (ci, release-please, release, etc.)
          • apps/ios/Gemfile + apps/ios/fastlane/*
          • pages/*.html, style.css, icons
          • release-please-config.json, .release-please-manifest.json
          • scripts/prompts/sentry-triage.md

        You'll still need to set GitHub secrets and create store listings
        before the pipeline will actually ship — see SETUP.md.

        Say no if you want to wire this up later (or never). You can always
        run the init script's CI-only opt-in manually later.
    """.trimIndent())
    println()
    print("Enable CI? (y/N): ")
    val answer = readln().trim().lowercase()
    val enable = answer == "y" || answer == "yes"

    if (!enable) {
        printYellow("   → CI skipped. Re-run setup from template/ci/ later if you change your mind.")
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
 * Substitute the {{APP_NAME}} / {{CONTACT_EMAIL}} / {{LAST_UPDATED}} /
 * {{APP_TAGLINE}} / {{APP_DESCRIPTION}} placeholders that live inside the
 * CI staging files. Runs after replaceFileContents so it applies to the
 * already-copied, already-renamed content.
 */
fun substitutePlaceholders(projectDir: File, projectName: ProjectName, contactEmail: String) {
    val today = java.time.LocalDate.now().toString()
    val tagline = "${projectName.displayName} — official site."
    val description = "${projectName.displayName} is a cross-platform app built with Kotlin Multiplatform and Compose."
    val pairs = listOf(
        "{{APP_NAME}}" to projectName.displayName,
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

fun cleanupTemplateArtifacts(projectDir: File, projectName: ProjectName) {
    val toDelete = listOf(
        "scripts/init_project.main.kts",
        "scripts/rename_to_template.sh"
    )
    toDelete.forEach { relative ->
        val file = File(projectDir, relative)
        if (file.exists()) {
            file.delete()
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

    content = content
        .replace(
            "# KMP Template\n\nA Kotlin Multiplatform template with",
            "# ${projectName.displayName}\n\nA Kotlin Multiplatform app with"
        )
        .replace("## Quick Start\n\n### Build & Run", "## Build & Run")

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

fun resetGitHistory(rootDir: File, projectName: ProjectName) {
    val gitDir = File(rootDir, ".git")
    if (gitDir.exists()) {
        gitDir.deleteRecursively()
        printGreen("   ✓ Removed old git history")
    }

    val result = ProcessBuilder("git", "init")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
        .waitFor()

    if (result == 0) {
        printGreen("   ✓ Initialized fresh git repository")

        ProcessBuilder("git", "add", ".")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .waitFor()

        ProcessBuilder("git", "commit", "-m", "Initial commit - ${projectName.displayName}")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
            .waitFor()

        printGreen("   ✓ Created initial commit")
    } else {
        printYellow("   ⚠ Could not initialize git (git may not be installed)")
    }
}

fun getProjectDir(projectName: ProjectName): File? {
    val templateDir = File(".").canonicalFile
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

    val projectDir = File(input.ifEmpty { suggestedPath }).canonicalFile

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
    
    val projectName = ProjectName.fromDisplayName(input)
    
    // Validate
    if (projectName.pascalCase.isEmpty() || !projectName.pascalCase[0].isLetter()) {
        printRed("❌ Invalid project name. Must start with a letter.")
        return null
    }
    
    if (!projectName.pascalCase.all { it.isLetterOrDigit() }) {
        printRed("❌ Invalid project name. Use only letters, numbers, and spaces.")
        return null
    }
    
    return projectName
}

fun getContactEmail(): String? {
    println()
    printCyan("""
        ✉️ Contact email

        Shown in the privacy/terms pages and used as the default support
        address. You can change it later by editing pages/*.html.

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
    
    val packageName = input.ifEmpty { suggestedPackage }
    
    // Validate package name
    val packageRegex = Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")
    if (!packageRegex.matches(packageName)) {
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

        // Specific template-framing phrases (before generic name replacements)
        "Guidelines for AI agents working in this KMP template repository." to
                "Guidelines for AI agents working in the ${projectName.displayName} repository.",
        "KMP (Kotlin Multiplatform) template with" to "KMP (Kotlin Multiplatform) app with",
        "this KMP template repository" to "this ${projectName.displayName} repository",

        // Name replacements in various formats
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
        "Podfile", "Gemfile", "Makefile", "Dockerfile", "gradlew"
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
    
    // Also handle package directory structure
    renamePackageDirectories(dir, packageName, stats)
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
    result = result.replace("kmptemplate", projectName.lowercase)
    
    return result
}

// Run the script
main()
