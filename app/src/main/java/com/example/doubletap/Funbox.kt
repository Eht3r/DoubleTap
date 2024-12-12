package com.example.doubletap

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// 여러번 쓰이는 함수 모음
class Funbox {

    // 폴더 압축
    fun compressFolderToZip(context: Context, folderUri: Uri, outputZipFile: File) {
        try {
            val fos = FileOutputStream(outputZipFile)
            val zos = ZipOutputStream(fos)

            // DocumentFile API를 사용하여 폴더 내부의 파일을 순회
            val folder = DocumentFile.fromTreeUri(context, folderUri)
            folder?.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileName = file.name
                    val entry = ZipEntry(fileName)
                    zos.putNextEntry(entry)

                    // ContentResolver를 사용하여 파일 내용 읽기
                    val inputStream = context.contentResolver.openInputStream(file.uri)
                    inputStream?.use {
                        it.copyTo(zos)
                    }

                    zos.closeEntry()
                }
            }

            zos.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 파일 공유
    @SuppressLint("QueryPermissionsNeeded")
    fun shareFile(context: Context, file: File) {
        Log.d("shareFile", file.absolutePath)
        // FileProvider를 사용하여 파일의 URI 생성
        val uri = FileProvider.getUriForFile(context, "com.example.doubletap.provider", file)

        var intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // Intent를 사용하여 공유 작업 시작
        val targetPackage = intent.resolveActivity(context.packageManager)?.packageName
        if (targetPackage != null) {
            context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // 공유할 앱 선택 창 표시
        context.startActivity(Intent.createChooser(intent, "공유"))
    }
}