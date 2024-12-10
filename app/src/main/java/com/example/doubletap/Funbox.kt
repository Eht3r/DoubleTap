package com.example.doubletap

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Funbox {
    // 폴더 압축
    fun compressFolderToZip(folder: File, outputZipFileDir: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFileDir))).use { zipOut ->
            compressFolder(folder, folder.name, zipOut)
        }
    }

    fun compressFolder(folder: File, parentPath: String, zipOut: ZipOutputStream) {
        folder.listFiles()?.forEach { file ->
            val zipEntryName = "$parentPath/${file.name}"
            if (file.isDirectory) {
                // 디렉토리일 경우 재귀적으로 처리
                compressFolder(file, zipEntryName, zipOut)
            } else {
                // 파일일 경우 zip에 추가
                FileInputStream(file).use { input ->
                    val entry = ZipEntry(zipEntryName)
                    zipOut.putNextEntry(entry)

                    val buffer = ByteArray(1024)
                    var length: Int
                    while (input.read(buffer).also { length = it} > 0) {
                        zipOut.write(buffer, 0, length)
                    }

                    zipOut.closeEntry()
                }
            }
        }
    }

    // 파일 공유
    fun shareFile(context: Context, file: File) {
        Log.d("shareFile", file.absolutePath)
        // FileProvider를 사용하여 파일의 URI 생성
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        // Intent를 사용하여 공유 작업 시작
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "zip") {
                "application/zip"
            } else {
                "application/markdown"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 공유할 앱 선택 창 표시
        context.startActivity(Intent.createChooser(intent, "공유"))
    }
}