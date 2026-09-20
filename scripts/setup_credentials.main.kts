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
 *   ./scripts/setup_credentials.main.kts           # walk through every value
 *   ./scripts/setup_credentials.main.kts --list    # show what is set, masked
 *   ./scripts/setup_credentials.main.kts --clear    # forget everything
 *
 * This is also the recovery path on a new machine: run it once and you are back
 * where you were.
 */

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
    else -> {
        red("Unknown option: ${args.first()}")
        println("Usage: setup_credentials.main.kts [--list|--clear]")
        exitProcess(2)
    }
}

// Explicit exit: the script JVM has been observed lingering after the last
// statement (a stray non-daemon thread keeps it alive), which hangs automation
// that waits on the process.
exitProcess(0)
