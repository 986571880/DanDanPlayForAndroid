package com.xyoye.stream_component.ui.fragment.storage_file

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.database.DatabaseManager
import com.xyoye.common_component.storage.Storage
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.utils.FileComparator
import com.xyoye.data_component.entity.PlayHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StorageFileFragmentViewModel : BaseViewModel() {
    private val hidePointFile = AppConfig.isShowHiddenFile().not()

    private val _fileLiveData = MutableLiveData<List<StorageFile>>()
    val fileLiveData: LiveData<List<StorageFile>> = _fileLiveData

    fun listFile(storage: Storage, directory: StorageFile?) {
        viewModelScope.launch(Dispatchers.IO) {
            val target = directory ?: storage.getRootFile()
            if (target == null) {
                _fileLiveData.postValue(emptyList())
                return@launch
            }

            val childFiles = storage.openDirectory(target)
                .filter {
                    isDisplayFile(it)
                }.sortedWith(
                    FileComparator({ it.fileName() }, { it.isDirectory() })
                ).onEach {
                    it.playHistory = getHistory(storage, it)
                }
            _fileLiveData.postValue(childFiles)
        }
    }

    fun unbindExtraSource(storage: Storage, file: StorageFile, unbindDanmu: Boolean) {
        viewModelScope.launch {
            if (unbindDanmu) {
                DatabaseManager.instance.getPlayHistoryDao().updateDanmu(
                    file.uniqueKey(), storage.library.mediaType, null, 0
                )
            } else {
                DatabaseManager.instance.getPlayHistoryDao().updateSubtitle(
                    file.uniqueKey(), storage.library.mediaType, null
                )
            }
            updateHistory(storage)
        }
    }

    fun updateHistory(storage: Storage) {
        viewModelScope.launch {
            val fileList = _fileLiveData.value ?: return@launch
            val newFileList = fileList.map {
                val history = getHistory(storage, it)
                if (it.playHistory == history) {
                    return@map it
                }
                //历史记录不一致时，返回拷贝的新对象
                it.clone().apply { playHistory = history }
            }
            _fileLiveData.postValue(newFileList)
        }
    }

    private suspend fun getHistory(storage: Storage, file: StorageFile): PlayHistoryEntity? {
        if (file.isDirectory()) {
            return null
        }
        if (file.isVideoFile().not()) {
            return null
        }
        return DatabaseManager.instance
            .getPlayHistoryDao()
            .getPlayHistory(
                file.uniqueKey(),
                storage.library.mediaType
            )
    }

    /**
     * 是否可展示的文件
     */
    private fun isDisplayFile(storageFile: StorageFile): Boolean {
        //.开头的文件，根据配置展示
        if (hidePointFile && storageFile.fileName().startsWith(".")) {
            return false
        }
        //文件夹，展示
        if (storageFile.isDirectory()) {
            return true
        }
        //视频文件，展示
        return storageFile.isVideoFile()
    }
}