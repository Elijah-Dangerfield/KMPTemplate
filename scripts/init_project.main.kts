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
const val TEMPLATE_PACKAGE = "com.kmptemplate"

// Extensions to process for content replacement
val TEXT_FILE_EXTENSIONS = setOf(
    "kt", "kts", "java", "xml", "json", "yaml", "yml", "md", "txt",
    "properties", "gradle", "swift", "h", "m", "plist", "entitlements",
    "xcconfig", "pbxproj", "xcscheme", "storyboard", "xib"
)

// Directories to skip during processing
val SKIP_DIRECTORIES = setOf(
    ".git", ".gradle", ".idea", "build", "node_modules", ".kotlin", 
    "caches", "generated", "intermediates"
)

// Files to skip
val SKIP_FILES = setOf(
    "init_project.main.kts", // Don't modify ourselves
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
        ║  This script will help you set up your new project by       ║
        ║  replacing all template placeholders with your project name ║
        ╚══════════════════════════════════════════════════════════════╝
    """.trimIndent())
    println()
    
    // Gather project information
    val projectName = getProjectName() ?: return
    val packageName = getPackageName(projectName) ?: return
    
    // Show summary and confirm
    println()
    printCyan("📋 Configuration Summary:")
    println("   Display Name:  ${projectName.displayName}")
    println("   PascalCase:    ${projectName.pascalCase}")
    println("   camelCase:     ${projectName.camelCase}")
    println("   lowercase:     ${projectName.lowercase}")
    println("   kebab-case:    ${projectName.kebabCase}")
    println("   Package:       $packageName")
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
    val rootDir = File(".").canonicalFile
    
    try {
        // Step 1: Replace file contents
        printBlue("📝 Step 1/3: Replacing file contents...")
        replaceFileContents(rootDir, projectName, packageName, stats)
        
        // Step 2: Rename directories (deepest first to avoid path issues)
        printBlue("📁 Step 2/3: Renaming directories...")
        renameDirectories(rootDir, projectName, packageName, stats)
        
        // Step 3: Rename files
        printBlue("📄 Step 3/3: Renaming files...")
        renameFiles(rootDir, projectName, stats)
        
        println()
        printGreen("✅ Project initialization complete!")
        println()
        printCyan("📊 Summary:")
        println("   Files modified:    ${stats.filesModified}")
        println("   Folders renamed:   ${stats.foldersRenamed}")
        println("   Files renamed:     ${stats.filesRenamed}")
        println("   Total replacements: ${stats.replacementsMade}")
        println()
        printYellow("📝 Next steps:")
        println("   1. Review the changes (git diff)")
        println("   2. Sync Gradle files in your IDE")
        println("   3. Build the project: ./gradlew build")
        println("   4. Update the README.md with your project details")
        println("   5. Delete this init script if you don't need it anymore")
        println()
        printGreen("🎉 Happy coding with ${projectName.displayName}!")
        
    } catch (e: Exception) {
        printRed("❌ Error during initialization: ${e.message}")
        e.printStackTrace()
        printYellow("⚠️  Some changes may have been partially applied. Check git status.")
    }
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

fun buildReplacements(projectName: ProjectName, packageName: String): List<Pair<String, String>> {
    return listOf(
        // Package replacements (most specific first)
        TEMPLATE_PACKAGE to packageName,
        
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

