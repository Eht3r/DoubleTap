package com.example.doubletap

import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doubletap.databinding.ActivityMainBinding
import java.io.File
import android.os.Environment
import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Adapter
    private var items = mutableListOf(Folder("폴더 추가", null, File("")))
    private val rootDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DoubleTap")

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 파일 관리자 등의 방법으로 폴더가 추가 되었을시 새로고침을 위한 코드
        binding.swipeLayout.setOnRefreshListener {
            loadFolders()
            binding.swipeLayout.isRefreshing = false
        }

        // 권한이 승인되엇는지 여부 확인후 아니라면 요청
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_PERMISSIONS
            )
        }

        // recyclerView에 adapter 연결
        adapter = Adapter(items) { folder ->
            if (folder.name == "폴더 추가") {
                showAddFolderDialog()
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // 상단바와 UI 요소 겹침 문제 해결
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemInsets.left,
                systemInsets.top,
                systemInsets.right,
                systemInsets.bottom
            )
            insets
        }

        loadFolders()

    }

    // 파일 시스쳄 사용을 위해서는 권한이 필요
    // 권한을 사용자로부터 요청하는 함수
    private fun allPermissionsGranted() = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // 폴더를 불러옴
    @SuppressLint("NotifyDataSetChanged")
    private fun loadFolders() {
        if (!rootDir.exists()) {
            rootDir.mkdir()
        }

        // 기존 아이템 초기화
        items.clear()
        items.add(Folder("폴더 추가", null, File("")))

        // 루트 디렉토리의 모든 폴더 찾기
        val folders = rootDir.listFiles { file -> file.isDirectory }?.map { file ->
            Folder(file.name, file.listFiles()?.size ?: 0, file)
        } ?: emptyList()

        // 발견된 폴더들을 리스트에 추가
        items.addAll(items.size - 1, folders)

        adapter.notifyDataSetChanged()
        Log.d("loadFolders", "Final items: ${items.map { it.name }}")
    }

    // 폴더 추가 다이얼로그
    private fun showAddFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edittext, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editText)
        val dialog = AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("새 폴더 추가")
            .setMessage("폴더 이름을 입력하세요")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val folderName = editText.text.toString()
                if (folderName.isNotEmpty()) {
                    addNewFolder(folderName)
                }
            }
            .setNegativeButton("취소", null)
            .create()
        dialog.show()
    }

    // 새로운 폴더를 추가
    private fun addNewFolder(folderName: String) {
        val newFolder = File(rootDir, folderName)
        if (newFolder.mkdir() || newFolder.exists()) {
            items.removeAll {it.name == folderName}
            val newItem = Folder(folderName, 0, newFolder)
            items.add(items.size - 1, newItem)
            adapter.notifyItemInserted(items.size - 2)
        } else {
            Log.e("addNewFolder", "Failed to create folder")
        }
    }
}