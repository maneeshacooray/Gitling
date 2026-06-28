package me.sheimi.sgit.activities.explorer

import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentManager
import com.manichord.mgit.MainActivity
import java.io.File
import java.io.FileFilter

/**
 * Shared state/behavior for a file-browsing screen, formerly a real Activity -- now a plain
 * ContextWrapper-based class constructed by one of MainActivity's NavHost routes ("exploreFile"
 * or "privateKeyManage"), as part of the single-activity rewrite. rootFolder/fileFilter/
 * screenTitle are constructor params rather than overridden methods (as they were when this was
 * an Activity) since calling an open/abstract method from a base class's own init block, before
 * the subclass's constructor has finished running, is exactly the kind of thing that breaks once
 * a subclass's override depends on anything set up in ITS OWN constructor.
 */
abstract class FileExplorerActivity(
    protected val mainActivity: MainActivity,
    private val rootFolder: File,
    private val fileFilter: FileFilter?,
    val screenTitle: String
) : ContextWrapper(mainActivity) {

    companion object {
        const val RESULT_PATH = "result_path"
    }

    var currentDirState by mutableStateOf(rootFolder)
    var filesState by mutableStateOf<List<File>>(emptyList())
    var selectedFile by mutableStateOf<File?>(null)

    val currentDir: File get() = currentDirState
    val isAtRoot: Boolean get() = currentDirState == rootFolder

    abstract fun onFileClick(file: File)
    open fun onFileLongClick(file: File) {}

    @Composable
    open fun TopBarActions() {}

    /** Optional overlay (e.g. a bottom sheet of actions for [selectedFile]) drawn on top of the list. */
    @Composable
    open fun Overlay() {}

    init {
        loadFiles()
    }

    fun setCurrentDir(dir: File) {
        currentDirState = dir
        loadFiles()
    }

    fun refreshList() {
        loadFiles()
    }

    /** Goes up a directory if not already at the root; returns false if there's nowhere left to
     * go (the caller should pop the NavHost route in that case). */
    fun goUpOrFalse(): Boolean {
        val parent = currentDir.parentFile
        return if (currentDir != rootFolder && parent != null) {
            setCurrentDir(parent)
            true
        } else {
            false
        }
    }

    private fun loadFiles() {
        val filter = fileFilter
        val files = (if (filter != null) currentDir.listFiles(filter) else currentDir.listFiles())
            ?: emptyArray()
        filesState = files.sortedWith(
            compareBy({ !it.isDirectory }, { it.toString() })
        )
    }

    fun getSupportFragmentManager(): FragmentManager = mainActivity.supportFragmentManager
}
