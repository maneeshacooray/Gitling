package me.sheimi.sgit.activities.explorer

import android.os.Environment
import com.manichord.mgit.MainActivity
import me.sheimi.sgit.R
import java.io.File
import java.io.FileFilter

class ExploreFileActivity(
    mainActivity: MainActivity,
    private val onFilePicked: (String) -> Unit
) : FileExplorerActivity(
    mainActivity,
    Environment.getExternalStorageDirectory(),
    FileFilter { file -> !file.name.startsWith(".") },
    mainActivity.getString(R.string.title_activity_explore_file)
) {
    override fun onFileClick(file: File) {
        if (file.isDirectory) {
            setCurrentDir(file)
            return
        }
        onFilePicked(file.absolutePath)
    }
}
