#!/usr/bin/env kotlin

@file:Import("lib/setup_store.main.kts")

/**
 * Fills in the machine-local credential store the other setup scripts read.
 *
 * The values here are per person and per machine, not per project: your Sentry
 * org, your Fly deploy tokens, your Apple team. Fill them in once and every
 * project you generate afterwards stops asking. Nothing here is required —
 * every script still prompts for whatever it cannot find.
 *
 *   ./scripts/setup_credentials.main.kts                 # walk through every value
 *   ./scripts/setup_credentials.main.kts --list          # show what is set, masked
 *   ./scripts/setup_credentials.main.kts --import <file> # read an env-format file
 *   ./scripts/setup_credentials.main.kts --clear         # forget everything
 *
 * This is also the recovery path on a new machine: run it once and you are back
 * where you were.
 */

import java.io.File
import kotlin.system.exitProcess

val store = SetupStore()

fun sourceOf(key: SetupKey): String? = when {
    System.getenv(key.env)?.isNotBlank() == true -> "environment"
    store[key] != null -> "stored"
    else -> null
}

fun show(key: SetupKey) {
    val environment = System.getenv(key.env)?.trim()?.takeIf { it.isNotEmpty() }
    val stored = store[key]
    val value = environment ?: stored
    val display = when {
        value == null -> "—"
        key.secret -> maskSecret(value)
        else -> value
    }
    val origin = sourceOf(key)?.let { " ($it)" } ?: ""
    println("  ${key.label.padEnd(42)} $display$origin")
    if (environment != null && stored != null && environment != stored) {
        yellow("      ${key.env} is set and differs from the stored value. The environment wins.")
    }
}

fun list() {
    bold("\nMachine-local setup credentials")
    println(store.file.path)
    if (!store.exists) yellow("  (not created yet)")
    println()
    Keys.all.forEach(::show)
    println()
    when (githubCliState()) {
        GithubCliState.READY -> green("  gh is installed and authenticated.")
        GithubCliState.NOT_AUTHENTICATED ->
            yellow("  gh is installed but not authenticated — run `gh auth login` so the " +
                "setup scripts can set repo secrets for you.")
        GithubCliState.NOT_INSTALLED ->
            yellow("  gh is not installed — the setup scripts will print the commands for " +
                "you to run by hand instead.")
    }
    println()
}

/**
 * Loads a `KEY=value` file into the store, matching on each key's environment
 * variable name.
 *
 * For the folder of shared release secrets people already keep — the one
 * `setup_github_secrets.main.kts` reads its files from usually has an env file
 * beside them. Those files name their values after the CI secrets, which is
 * exactly what [SetupKey.env] is, so most of them land with no typing.
 *
 * Sourcing that file into the shell instead would *look* like it worked — every
 * value would show as set — but the editor skips anything already in the
 * environment, on purpose, so nothing would persist and the next shell would be
 * empty again. Hence an explicit import that writes.
 */
fun import(path: String) {
    val file = File(path.replaceFirst("~", System.getProperty("user.home")))
    if (!file.isFile) die("Not a file: ${file.absolutePath}")

    val parsed = file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
        .associate {
            it.substringBefore('=').trim().removePrefix("export ").trim() to
                it.substringAfter('=').trim().trim('\'', '"')
        }
        .filterValues { it.isNotEmpty() }

    bold("\nImporting from ${file.absolutePath}")
    var imported = 0
    for (key in Keys.all) {
        val value = parsed[key.env] ?: continue
        if (store[key] == value) {
            dim("  ${key.label} — already stored, unchanged")
            continue
        }
        val existing = store[key]
        if (existing != null && !confirm("  Replace ${key.label}?", default = false)) continue
        store[key] = value
        imported++
        green("  ✓ ${key.label} ← ${key.env}")
    }

    imported += importFilePaths(file, parsed)

    val handled = Keys.all.map { it.env }.toSet()
    val unknown = parsed.keys.filterNot { it in handled || it.startsWith("FILE_") }
    if (unknown.isNotEmpty()) {
        println()
        dim("  Ignored (not values this store holds): ${unknown.sorted().joinToString(", ")}")
    }

    println()
    if (imported == 0) green("Nothing new to import.") else green("✓ Imported $imported value(s).")
}

/**
 * Derives the two path-shaped store values from `FILE_*` entries.
 *
 * A folder of shared release secrets tends to name its binary material this
 * way, with paths relative to the env file and sometimes a glob, because Apple
 * bakes an unpredictable key id into the `.p8` filename. The store wants the
 * *folder* rather than four paths — `setup_github_secrets.main.kts` finds the
 * files inside it — so the common parent is what gets kept.
 *
 * Nothing is read from these files here. Only where they are.
 */
fun importFilePaths(envFile: File, parsed: Map<String, String>): Int {
    /** Resolves relative to the env file, expanding a `*` in the filename. */
    fun resolve(path: String): File? {
        val raw = File(path.replaceFirst("~", System.getProperty("user.home")))
        val absolute = if (raw.isAbsolute) raw else File(envFile.parentFile, path)
        if (!absolute.name.contains('*')) return absolute.takeIf { it.isFile }
        // Regex.escape wraps the name in \Q…\E, so the `*` inside it is a plain
        // character — break out of the quote around it rather than escaping it.
        val pattern = Regex(Regex.escape(absolute.name).replace("*", "\\E.*\\Q"))
        return absolute.parentFile?.listFiles()
            ?.filter { it.isFile && pattern.matches(it.name) }
            ?.singleOrNull()
    }

    val resolved = parsed.filterKeys { it.startsWith("FILE_") }
        .mapNotNull { (name, path) -> resolve(path)?.let { name to it } }
        .toMap()
    if (resolved.isEmpty()) return 0

    var imported = 0

    fun store(key: SetupKey, value: String, from: String) {
        if (store[key] == value) return
        if (store[key] != null && !confirm("  Replace ${key.label}?", default = false)) return
        store[key] = value
        imported++
        green("  ✓ ${key.label} ← $from")
    }

    // One shared parent or nothing: files scattered across directories are not
    // a signing folder, and guessing one of them would send the release script
    // somewhere it would find half the material.
    resolved.values.map { it.parentFile }.distinctBy { it.canonicalPath }.singleOrNull()
        ?.let { store(Keys.SIGNING_DIR, it.canonicalPath, "the folder FILE_* point into") }

    resolved["FILE_ASC_KEY"]
        ?.let { store(Keys.ASC_KEY_PATH, it.canonicalPath, "FILE_ASC_KEY") }

    return imported
}

fun clear() {
    if (!store.exists) {
        yellow("Nothing to clear — ${store.file.path} does not exist.")
        return
    }
    if (!confirm("Delete every stored value in ${store.file.path}?", default = false)) {
        yellow("Left alone.")
        return
    }
    Keys.all.forEach(store::remove)
    green("✓ Cleared. Every setup script will go back to prompting.")
}

fun edit() {
    bold("\n━━ Setup credentials ━━")
    println(
        """
        These are the values that are the same for every project you generate
        from this template. Fill in what you have; press Enter to skip anything
        you do not. Nothing here is required — a script that cannot find a value
        asks for it, exactly as it does today.

        Stored at ${store.file.path}, owner-readable only, outside every repo.
        """.trimIndent()
    )

    var saved = 0
    for (key in Keys.all) {
        println()
        bold(key.label)
        dim("  ${key.where}")
        val environment = System.getenv(key.env)?.trim()?.takeIf { it.isNotEmpty() }
        if (environment != null) {
            // Env wins at read time, so storing a different value here would be
            // a value that is set and also ignored — the exact confusion this
            // store is supposed to remove.
            yellow("  ${key.env} is set in this shell and takes precedence over the store. Skipping.")
            continue
        }
        val existing = store[key]
        if (existing != null) {
            println("  Currently: ${if (key.secret) maskSecret(existing) else existing}")
            if (!confirm("  Replace it?", default = false)) continue
        }
        // Not promptSecret: skipping has to stay possible, and a hidden prompt
        // that accepts empty input cannot tell "skip" from "typed nothing".
        print("  New value (Enter to ${if (existing != null) "keep" else "skip"}): ")
        System.out.flush()
        val input = (readlnOrNull() ?: break).trim()
        if (input.isEmpty()) continue
        store[key] = input
        saved++
        green("  ✓ Saved")
    }

    println()
    if (saved == 0) {
        yellow("Nothing changed.")
    } else {
        green("✓ Saved $saved value(s) to ${store.file.path}")
    }

    val missing = Keys.all.filter { sourceOf(it) == null }
    if (missing.isNotEmpty()) {
        bold("\nStill unset")
        for (key in missing) {
            yellow("  • ${key.label}")
            println("      ${key.consequence}")
        }
    }

    println()
    dim(
        "Moving to a new machine: rerun this script there. Copying the file directly " +
            "works too, but it holds live deploy tokens in plain text — treat that copy " +
            "like the tokens themselves, and do not put it anywhere that syncs."
    )
}

when (args.firstOrNull()) {
    null -> { edit(); list() }
    "--list", "-l" -> list()
    "--clear" -> clear()
    "--import" -> {
        val path = args.getOrNull(1) ?: die("--import needs a file path")
        import(path)
        list()
    }
    else -> {
        red("Unknown option: ${args.first()}")
        println("Usage: setup_credentials.main.kts [--list|--clear|--import <file>]")
        exitProcess(2)
    }
}

// Explicit exit: the script JVM has been observed lingering after the last
// statement (a stray non-daemon thread keeps it alive), which hangs automation
// that waits on the process.
exitProcess(0)
