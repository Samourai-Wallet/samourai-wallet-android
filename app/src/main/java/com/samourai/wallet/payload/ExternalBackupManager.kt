package com.samourai.wallet.payload

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.R
import com.samourai.wallet.util.PrefsUtil
import kotlinx.coroutines.*
import java.io.File

/**
 * samourai-wallet-android
 *
 * Utility for managing android scope storage and legacy storage for external backups
 *
 * Refs:
 * https://developer.android.com/about/versions/11/privacy/storage
 * https://developer.android.com/guide/topics/permissions/overview
 */
object ExternalBackupManager {

    private lateinit var appContext: Application
    private const val strOptionalBackupDir = "/samourai"
    private const val STORAGE_REQ_CODE = 4866
    private const val READ_WRITE_EXTERNAL_PERMISSION_CODE = 2009
    private var backUpDocumentFile: DocumentFile? = null
    private const val strBackupFilename = "samourai.txt"
    private val permissionState = MutableLiveData(false)
    private val scope = CoroutineScope(Dispatchers.Main) + SupervisorJob()


    /**
     * Shows proper dialog for external storage permission
     *
     * Invokes API specific storage requests.
     * scoped storage for API 29
     * normal external storage request for API below 29
     */
    @JvmStatic
    fun askPermission(activity: Activity) {

        fun ask() {
            if (requireScoped()) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                }
                activity.startActivityForResult(intent, STORAGE_REQ_CODE)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestPermissions(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ), READ_WRITE_EXTERNAL_PERMISSION_CODE
                    )
                }
            }
        }

        var titleId = R.string.permission_alert_dialog_title_external
        var message = appContext.getString(R.string.permission_dialog_message_external)
        if (requireScoped()) {
            titleId = R.string.permission_alert_dialog_title_external_scoped
            message = appContext.getString(R.string.permission_dialog_scoped)
        }
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(titleId)
            .setMessage(message)
            .setPositiveButton(if (requireScoped()) R.string.choose else R.string.ok) { dialog, _ ->
                dialog.dismiss()
                ask()
            }.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    /**
     * Attach to root application object to retrieve context
     * Ref: [com.samourai.wallet.SamouraiApplication] onCreate
     */
    @JvmStatic
    fun attach(application: Application) {
        this.appContext = application
        if (requireScoped()) {
            this.initScopeStorage()
        }
    }

    @JvmStatic
    fun write(content: String) {
        scope.launch(Dispatchers.IO) {
            try {
                if (requireScoped()) {
                    writeScopeStorage(content)
                } else {
                    writeLegacyStorage(content)
                }
            } catch (e: Exception) {
                throw CancellationException(e.message)
            }
        }
    }

    @JvmStatic
    fun read(): String? = if (requireScoped()) {
        readScoped()
    } else {
        readLegacy()
    }

    private fun initScopeStorage() {
        if (PrefsUtil.getInstance(appContext).has(PrefsUtil.BACKUP_FILE_PATH)) {
            val path: String =
                PrefsUtil.getInstance(appContext).getValue(PrefsUtil.BACKUP_FILE_PATH, "");
            if (path.isNotEmpty()) {
                if (DocumentFile.fromTreeUri(appContext, path.toUri()) == null) {
                    permissionState.postValue(false)
                    return
                }
                val documentsTree = DocumentFile.fromTreeUri(appContext, path.toUri())!!

                var hasPerm = false

                appContext.contentResolver.persistedUriPermissions.forEach { uri ->
                    if (uri.uri.toString() == path) {
                        permissionState.postValue(true)
                        hasPerm = true
                    }
                }

                if (!hasPerm) {
                    return
                }

                documentsTree.listFiles().forEach { doc ->
                    if (BuildConfig.FLAVOR == "staging") {
                        if (doc.isDirectory && doc.name == "staging") {
                            doc.findFile(strBackupFilename)?.let {
                                backUpDocumentFile = it
                            }
                        }
                    } else {
                        if (doc.isFile && doc.name == strBackupFilename) {
                            backUpDocumentFile = doc
                        }
                    }
                }

                if (backUpDocumentFile == null) {
                    backUpDocumentFile = if (BuildConfig.FLAVOR == "staging") {
                        val stagingDir = documentsTree.createDirectory("staging")
                        stagingDir?.createFile("text/plain ", strBackupFilename)
                    } else {
                        documentsTree.createFile("text/plain ", strBackupFilename)
                    }
                }
            }
        }
    }

    @JvmStatic
    private fun writeScopeStorage(content: String) {

        if (backUpDocumentFile == null) {
            throw  Exception("Backup file not available")
        }
        if (!backUpDocumentFile!!.canRead()) {
            throw  Exception("Backup file is not readable")
        }
        val stream = appContext.contentResolver.openOutputStream(backUpDocumentFile!!.uri)
        stream?.write(content.encodeToByteArray())
    }

    @JvmStatic
    private fun writeLegacyStorage(content: String) {
        if (hasPermission()) {
            if (!getLegacyBackupFile().exists()) {
                getLegacyBackupFile().createNewFile()
            }
            getLegacyBackupFile().writeText(content)
        } else {
            throw  Exception("Backup file not available")
        }
    }

    @JvmStatic
    fun readScoped(): String? {
        if (backUpDocumentFile == null) {
            throw  Exception("Backup file not available")
        }
        if (!backUpDocumentFile!!.canRead()) {
            throw  Exception("Backup file is not readable")
        }

        val stream = appContext.contentResolver.openInputStream(backUpDocumentFile!!.uri)
        return stream?.readBytes()?.decodeToString()
    }

    private fun readLegacy(): String? {
        return if (hasPermission()) {
            getLegacyBackupFile().readText()
        } else {
            null
        }
    }


    @JvmStatic
    fun lastUpdated(): Long? {
        return if (requireScoped()) {
            backUpDocumentFile?.lastModified()
        } else {
            if (hasPermission() && getLegacyBackupFile().exists()) {
                getLegacyBackupFile().lastModified()
            } else {
                null
            }
        }
    }

    /**
     * Checks both scoped and non-scoped storage permissions
     *
     * For scoped storage method will use persistedUriPermissions array from
     * contentResolver to compare allowed path that store in the prefs
     *
     */
    @JvmStatic
    fun hasPermissions(): Boolean {
        if (requireScoped()) {
            if (backUpDocumentFile == null) {
                return false
            }
            val path: String =
                PrefsUtil.getInstance(appContext).getValue(PrefsUtil.BACKUP_FILE_PATH, "");
            appContext.contentResolver.persistedUriPermissions.forEach { uri ->
                if (uri.uri.toString() == path) {
                    return true
                }
            }
            return false
        } else {
            return hasPermission()
        }
    }

    @JvmStatic
    fun backupAvailable(): Boolean {
        if (requireScoped()) {
            if (backUpDocumentFile == null ) {
                return false
            }
            if (backUpDocumentFile!!.canRead()) {
                return backUpDocumentFile!!.exists()
            }
            return false
        } else {
            return getLegacyBackupFile().exists()
        }
    }

    /**
     * Handles permission result that received in an activity
     *
     * Any Activity that using this class should invoke this method in onActivityResult
     */
    @JvmStatic
    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        application: Application
    ) {
        val directoryUri = data?.data ?: return
        if (requestCode == STORAGE_REQ_CODE && resultCode == RESULT_OK) {
            PrefsUtil.getInstance(application)
                .setValue(PrefsUtil.BACKUP_FILE_PATH, directoryUri.toString())
            this.appContext.contentResolver.takePersistableUriPermission(
                directoryUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            this.attach(application)
            permissionState.postValue(true)
        }
    }

    /**
     * For older api's ( below API 29)
     */
    @Suppress("DEPRECATION")
    private fun getLegacyBackupFile(): File {
        val directory = Environment.DIRECTORY_DOCUMENTS
        val dir: File? = if (appContext.packageName.contains("staging")) {
            Environment.getExternalStoragePublicDirectory("$directory$strOptionalBackupDir/staging")
        } else {
            Environment.getExternalStoragePublicDirectory("$directory$strOptionalBackupDir")
        }
        if (!dir?.exists()!!) {
            dir.mkdirs()
            dir.setWritable(true)
            dir.setReadable(true)
        }
        val backupFile = File(dir, strBackupFilename);
        return backupFile
    }

    private fun hasPermission(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writePerm = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return (readPerm && writePerm)
    }

    private fun requireScoped() = Build.VERSION.SDK_INT >= 29

    @JvmStatic
    fun getPermissionStateLiveData(): LiveData<Boolean> {
        return permissionState
    }

    @JvmStatic
    fun dispose() {
        if (scope.isActive) {
            scope.cancel()
        }
    }

}