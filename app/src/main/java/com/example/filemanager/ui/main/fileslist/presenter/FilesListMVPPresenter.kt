package com.example.filemanager.ui.main.fileslist.presenter

import com.example.filemanager.base.presenter.MVPPresenter
import com.example.filemanager.base.view.MVPView

interface FilesListMVPPresenter<V : MVPView> : MVPPresenter<V>{

  //  fun loadItems(path :String) : MutableList<FileModel>
}