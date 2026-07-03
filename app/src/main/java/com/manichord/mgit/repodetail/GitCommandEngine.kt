package com.manichord.mgit.repodetail

import me.sheimi.sgit.database.models.Repo
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GitCommandEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun execute(repo: Repo, input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val cmd = trimmed.removePrefix("git ").trim()
        val parts = tokenize(cmd)
        if (parts.isEmpty()) return ""
        return try {
            val git = repo.getGit()
            when (parts[0]) {
                "status" -> runStatus(repo)
                "log" -> runLog(repo, parts.drop(1))
                "diff" -> runDiff(repo, parts.drop(1))
                "branch" -> runBranch(repo, parts.drop(1))
                "stash" -> runStash(repo, parts.drop(1))
                "help", "--help", "-h" -> helpText()
                else -> "unknown command: ${parts[0]}\n\n${helpText()}"
            }
        } catch (e: Exception) {
            "error: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun runStatus(repo: Repo): String {
        val git = repo.getGit()
        val branchName = git.repository.branch ?: "HEAD"
        val status = git.status().call()

        val sb = StringBuilder()
        sb.appendLine("On branch $branchName")

        val staged = status.added + status.changed + status.removed
        if (staged.isNotEmpty()) {
            sb.appendLine("\nChanges to be committed:")
            status.added.sorted().forEach { sb.appendLine("        new file:   $it") }
            status.changed.sorted().forEach { sb.appendLine("        modified:   $it") }
            status.removed.sorted().forEach { sb.appendLine("        deleted:    $it") }
        }

        val unstaged = status.modified + status.missing
        if (unstaged.isNotEmpty()) {
            sb.appendLine("\nChanges not staged for commit:")
            status.modified.sorted().forEach { sb.appendLine("        modified:   $it") }
            status.missing.sorted().forEach { sb.appendLine("        deleted:    $it") }
        }

        if (status.untracked.isNotEmpty()) {
            sb.appendLine("\nUntracked files:")
            status.untracked.sorted().forEach { sb.appendLine("        $it") }
        }

        if (staged.isEmpty() && unstaged.isEmpty() && status.untracked.isEmpty()) {
            sb.append("nothing to commit, working tree clean")
        }
        return sb.toString().trimEnd()
    }

    private fun runLog(repo: Repo, args: List<String>): String {
        val git = repo.getGit()
        val oneline = "--oneline" in args
        val graph = "--graph" in args
        val limit = args.firstOrNull { it.startsWith("-") && it.drop(1).all(Char::isDigit) }
            ?.drop(1)?.toIntOrNull()
            ?: args.indexOf("-n").takeIf { it >= 0 }?.let { args.getOrNull(it + 1)?.toIntOrNull() }
            ?: 20
        val since = args.firstOrNull { it.startsWith("--since=") }
            ?.removePrefix("--since=")?.let { parseSinceDate(it) }

        val logCmd = git.log().setMaxCount(if (since != null) 500 else limit)
        val commits = logCmd.call().toList()

        val filtered = if (since != null) {
            commits.filter { it.commitTime.toLong() * 1000 >= since }.take(limit)
        } else commits

        if (filtered.isEmpty()) return "(no commits)"

        val sb = StringBuilder()
        val prefix = if (graph) "* " else ""
        filtered.forEach { commit ->
            val sha = commit.name.take(7)
            val date = dateFormat.format(Date(commit.commitTime.toLong() * 1000))
            val author = commit.authorIdent.name
            val msg = commit.shortMessage
            if (oneline) {
                sb.appendLine("$prefix$sha $msg")
            } else {
                sb.appendLine("${prefix}commit ${commit.name}")
                sb.appendLine("Author: $author")
                sb.appendLine("Date:   $date")
                sb.appendLine()
                sb.appendLine("    $msg")
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }

    private fun runDiff(repo: Repo, args: List<String>): String {
        val git = repo.getGit()
        val staged = "--staged" in args || "--cached" in args
        val out = ByteArrayOutputStream()
        val formatter = DiffFormatter(out).apply {
            setRepository(git.repository)
            setDiffComparator(RawTextComparator.DEFAULT)
            isDetectRenames = true
        }
        val diffs = git.diff().setCached(staged).call()
        if (diffs.isEmpty()) return if (staged) "(no staged changes)" else "(no unstaged changes)"

        val sb = StringBuilder()
        diffs.take(20).forEach { entry ->
            sb.appendLine("diff --git a/${entry.oldPath} b/${entry.newPath}")
            sb.appendLine("--- a/${entry.oldPath}")
            sb.appendLine("+++ b/${entry.newPath}")
            try {
                out.reset()
                formatter.format(entry)
                val patch = out.toString()
                val hunks = patch.lines().filter { it.startsWith("@@") || it.startsWith("+") || it.startsWith("-") }
                hunks.take(30).forEach { sb.appendLine(it) }
                if (hunks.size > 30) sb.appendLine("... (${hunks.size - 30} more lines)")
            } catch (_: Exception) { }
            sb.appendLine()
        }
        if (diffs.size > 20) sb.appendLine("... and ${diffs.size - 20} more file(s)")
        formatter.close()
        return sb.toString().trimEnd()
    }

    private fun runBranch(repo: Repo, args: List<String>): String {
        val git = repo.getGit()
        val mode = when {
            "-a" in args -> ListBranchCommand.ListMode.ALL
            "-r" in args -> ListBranchCommand.ListMode.REMOTE
            else -> null
        }
        val current = git.repository.branch
        val branches = git.branchList().apply { mode?.let { setListMode(it) } }.call()
        if (branches.isEmpty()) return "(no branches)"
        val sb = StringBuilder()
        branches.forEach { ref ->
            val name = ref.name
                .removePrefix("refs/heads/")
                .removePrefix("refs/remotes/")
            val marker = if (ref.name == "refs/heads/$current") "* " else "  "
            sb.appendLine("$marker$name")
        }
        return sb.toString().trimEnd()
    }

    private fun runStash(repo: Repo, args: List<String>): String {
        val git = repo.getGit()
        val sub = args.firstOrNull() ?: "list"
        return when (sub) {
            "list" -> {
                val stashes = git.stashList().call()
                if (stashes.isEmpty()) return "(no stashes)"
                stashes.mapIndexed { i, c -> "stash@{$i}: ${c.shortMessage}" }.joinToString("\n")
            }
            "save", "push", "" -> {
                val ref = git.stashCreate().call()
                if (ref == null) "nothing to stash" else "Saved working directory and index state\n${ref.name.take(7)}"
            }
            "pop" -> {
                val stashes = git.stashList().call().toList()
                if (stashes.isEmpty()) return "No stash entries found"
                git.stashApply().setStashRef("stash@{0}").call()
                git.stashDrop().setStashRef(0).call()
                "Applied and dropped stash@{0}: ${stashes[0].shortMessage}"
            }
            "drop" -> {
                val index = args.getOrNull(1)?.removePrefix("stash@{")?.removeSuffix("}")?.toIntOrNull() ?: 0
                val stashes = git.stashList().call().toList()
                if (index >= stashes.size) return "No stash entry at index $index"
                git.stashDrop().setStashRef(index).call()
                "Dropped stash@{$index}: ${stashes[index].shortMessage}"
            }
            else -> "unknown stash subcommand: $sub\nUsage: stash [list|push|pop|drop [stash@{n}]]"
        }
    }

    private fun helpText() = """
supported commands:
  status              show working tree status
  log                 show commit history
    --oneline         compact one-line format
    --graph           with branch graph prefix
    -n <N> or -<N>    limit number of commits (default 20)
    --since=<date>    e.g. --since=yesterday, --since=2025-01-01
  diff                show unstaged changes
    --staged          show staged changes
  branch              list local branches
    -a                list all branches (local + remote)
    -r                list remote branches only
  stash               stash / list / pop / drop
    list              list stash entries
    push              save working tree to stash
    pop               apply and drop stash@{0}
    drop [stash@{n}]  drop a specific stash entry
  help                show this help
    """.trimIndent()

    private fun parseSinceDate(since: String): Long? {
        if (since == "yesterday") {
            return System.currentTimeMillis() - 86_400_000L
        }
        val fmt1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fmt2 = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return runCatching { fmt2.parse(since)?.time }
            .getOrNull()
            ?: runCatching { fmt1.parse(since)?.time }.getOrNull()
    }

    private fun tokenize(cmd: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote = false
        var quoteChar = ' '
        for (ch in cmd) {
            when {
                inQuote && ch == quoteChar -> inQuote = false
                !inQuote && (ch == '"' || ch == '\'') -> { inQuote = true; quoteChar = ch }
                !inQuote && ch == ' ' -> {
                    if (current.isNotEmpty()) { result.add(current.toString()); current.clear() }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }
}
