package com.example.filemanager.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import com.example.filemanager.R
import com.example.filemanager.base.model.FileModel
import com.example.filemanager.base.view.BaseActivity
import com.example.filemanager.base.view.BaseFragment
import com.example.filemanager.ui.main.fileslist.view.FilesListFragment
import com.example.filemanager.ui.main.fileslist.view.FilesRecyclerViewAdapter
import com.example.filemanager.utils.FileType
import com.example.filemanager.utils.launchFileIntent
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject
import android.view.Menu
import android.widget.Toast
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.example.filemanager.ui.settings.SettingsActivity

class MainActivity : BaseActivity(), HasSupportFragmentInjector, FilesListFragment.OnItemClickListener{

    private lateinit var files : MutableList<FileModel>

    @Inject
    internal lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    private val backStackManager = BackStackManager()
    private lateinit var fileListRecyclerViewAdapter: FilesRecyclerViewAdapter

    override fun supportFragmentInjector() = fragmentDispatchingAndroidInjector
    override fun getDefaultFragment(): BaseFragment = FilesListFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val defaultFolderPath =
            sharedPreferences.getString("path", Environment.getExternalStorageDirectory().absolutePath)

        if (savedInstanceState == null) {
            val filesListFragment = FilesListFragment.build {
                if (defaultFolderPath != null) path = defaultFolderPath
            }


            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, filesListFragment)
                .addToBackStack(Environment.getExternalStorageDirectory().absolutePath)
                .commit()
        }

        initBackStack()

    }

    private fun initBackStack() {
        backStackManager.onStackChangeListener = {
        }

        backStackManager.addToStack(fileModel = FileModel(Environment.getExternalStorageDirectory().absolutePath, FileType.FOLDER, "/", 0.0))
    }

    override fun onFragmentAttached() {
    }

    override fun onFragmentDetached(tag: String) {
    }

    private fun addFileFragment(fileModel: FileModel) {
        val filesListFragment = FilesListFragment.build {
            path = fileModel.path
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, filesListFragment)
        fragmentTransaction.addToBackStack(fileModel.path)
        fragmentTransaction.commit()
    }

    override fun onClick(fileModel: FileModel) {
        if (fileModel.fileType == FileType.FOLDER) {
            addFileFragment(fileModel)
        }
        else{
            launchFileIntent(fileModel)
        }
    }

    override fun onLongClick(fileModel: FileModel) {

    }


    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when(id){
            R.id.action_refresh -> Toast.makeText(this@MainActivity, "List automatically refreshed (AutoUpdatableAdapter)", Toast.LENGTH_LONG).show()
            R.id.action_settings ->{
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
