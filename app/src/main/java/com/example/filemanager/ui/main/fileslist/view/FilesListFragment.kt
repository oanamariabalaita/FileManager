package com.example.filemanager.ui.main.fileslist.view

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filemanager.R
import com.example.filemanager.base.model.FileModel
import com.example.filemanager.base.view.BaseFragment
import com.example.filemanager.ui.main.MainActivity
import com.example.filemanager.ui.main.fileslist.presenter.FilesListMVPPresenter
import com.example.filemanager.utils.PermissionManager
import com.example.filemanager.utils.getFileModelsFromFiles
import com.example.filemanager.utils.getFilesFromPath
import kotlinx.android.synthetic.main.fragment_files_list.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import javax.inject.Inject


class FilesListFragment : BaseFragment(), FilesListMVPView {

    override fun openSettingsActivity() {
    }

    companion object {
        internal const val ARG_PATH: String = "com.example.filemanager.fileslist.path"
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build(ARG_PATH)

        private const val REQUEST_READ_EXTERNAL_STORAGE = 1
    }

    @Inject
    internal lateinit var presenter: FilesListMVPPresenter<FilesListMVPView>

    private val filesListRvAdapter: FilesRecyclerViewAdapter by lazy { FilesRecyclerViewAdapter() }
    private var filesList = mutableListOf<FileModel>()
    private lateinit var mCallback: OnItemClickListener
    private lateinit var permissionManager: PermissionManager

    interface OnItemClickListener {
        fun onClick(fileModel: FileModel)

        fun onLongClick(fileModel: FileModel)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        permissionManager = PermissionManager(context)

        context.let {
            if (checkSelfPermission(
                    it,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(
                    it,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                askForMultiplePermissions()
            } else {
                loadFiles()
            }
        }

        try {
            mCallback = context as OnItemClickListener
        } catch (e: Exception) {
            throw Exception("$context should implement FilesListFragment.OnItemCLickListener")
        }
    }

    private fun loadFiles() {
        doAsync {
            arguments?.getString(ARG_PATH)?.let {
                filesList = getFileModelsFromFiles(getFilesFromPath(it))
            }

            uiThread {
                filesListRvAdapter.refreshList(filesList)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_files_list, container, false)
    }

    private fun initViews() {

        context?.let { ctx ->

            setOrientation(resources.configuration.orientation)

            filesListRvAdapter.onItemClickListener = { file, itemView: View, _: Int ->
                mCallback.onClick(file)

            }
            filesListRvAdapter.onItemLongClickListener = { file, _ ->
                mCallback.onLongClick(file)
                true
            }
        }
    }


    //called in onViewCreated
    override fun setUp() {
        initViews()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setOrientation(newConfig.orientation)
    }


    private fun setOrientation(orientation: Int) {
        context?.let { ctx ->
            mainRecycleView?.apply {
                layoutManager = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    GridLayoutManager(ctx, 2)
                } else {
                    LinearLayoutManager(ctx)
                }
                adapter = filesListRvAdapter
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it,
                permission
            )
        } == PackageManager.PERMISSION_GRANTED
    }

    private fun askForMultiplePermissions() {
        val requestCode = 13
        val writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val readExternalStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
        val permissionList = arrayListOf<String>()

        if (!hasPermission(writeExternalStorage)) {
            permissionList.add(writeExternalStorage)
        }
        if (!hasPermission(readExternalStoragePermission)) {
            permissionList.add(readExternalStoragePermission)
        }
        if (permissionList.isNotEmpty()) {
            val permissions = permissionList.toTypedArray()
            requestPermissions(permissions, requestCode)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {

        when (requestCode) {
            13 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==PackageManager.PERMISSION_GRANTED) {
                    //Permissions were granted

                    // app restart necessary
                    val mStartActivity = Intent(context, MainActivity::class.java)
                    val mPendingIntentId = 123456
                    val mPendingIntent = PendingIntent.getActivity(
                        context,
                        mPendingIntentId,
                        mStartActivity,
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    val mgr = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis(), mPendingIntent)
                    System.exit(0)


                } else {
                    Toast.makeText(context,"For using this app grant the storage permissions!", Toast.LENGTH_LONG).show()
                }
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onDestroy() {
        presenter.onDetach()
        super.onDestroy()
    }

    class Builder {
        var path: String = ""

        fun build(s: String): FilesListFragment {
            val fragment = FilesListFragment()
            val args = Bundle()
            args.putString(s, path)
            fragment.arguments = args
            return fragment
        }
    }
}

