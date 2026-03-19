package com.kmptemplate.libraries.ui

interface PhotoSaver {
    suspend fun savePhoto(photoData: ByteArray): String?
}
