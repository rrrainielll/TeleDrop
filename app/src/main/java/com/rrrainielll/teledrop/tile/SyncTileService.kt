package com.rrrainielll.teledrop.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rrrainielll.teledrop.worker.UploadWorker

class SyncTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_IS_AUTO_SYNC to true))
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("sync_work")
            .build()

        WorkManager.getInstance(applicationContext).enqueue(syncRequest)

        // Reset state to ensure it's clickable again
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.label = "TeleDrop Sync"
        qsTile.updateTile()
    }
}
