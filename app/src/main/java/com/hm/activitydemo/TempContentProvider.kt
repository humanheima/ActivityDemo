package com.hm.activitydemo

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * Created by p_dmweidu on 2025/2/11
 * Desc:
 */
class TempContentProvider : ContentProvider() {
    private val TAG = "TempContentProvider"

    override fun onCreate(): Boolean {
        Log.e(TAG, "onCreate:${Log.getStackTraceString(Throwable())} ")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }


    override fun getType(uri: Uri): String? {
        return ""
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }


}