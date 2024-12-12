package com.example.doubletap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doubletap.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MainAdapter
    var items = mutableListOf(Folder(File("")))
    private val rootDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        "DoubleTap"
    )

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private const val REQUEST_CODE_OPEN_DOCUMENT_TREE = 1002
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
        adapter = MainAdapter(items) { folder ->
            if (folder.name == "폴더 추가") {
                showAddFolderDialog()
            } else {
                val dir = folder.folderDir
                Log.d("MainActivity", "Selected folder: $dir")
                val intent = Intent(this, FileListActivity::class.java)
                intent.putExtra("rootDir", rootDir.absolutePath)
                intent.putExtra("folderDir", dir)
                intent.putExtra("folderName", folder.name)
                startActivity(intent)
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // 상단바와 UI 요소 겹침 문제 해결
        ViewCompat.setOnApplyWindowInsetsListener(binding.swipeLayout) { view, insets ->
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

        // 스와이프 기능 추가
        val swipeController = SwipeController(object : SwipeControllerActions {
            override fun onLeftClicked(position: Int) {
                // 선택한 폴더 가져오기
                val folder = items[position].folder
                // 압축 파일 경로
                val outputZipFileDir = File(rootDir, "${folder.name}.zip")
                // 폴더에 대한 접근 권한 요청
                requestFolderAccess(folder)
            }

            override fun onRightClicked(position: Int) {
                adapter.deleteItem(position)
            }
        })
        val itemTouchHelper = ItemTouchHelper(swipeController)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
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

        // 폴더 추가 요소
        val addFolder = Folder(File(""))

        // 기존 아이템 초기화
        items.clear()
        items.add(addFolder)

        // 루트 디렉토리의 모든 폴더 찾기
        val folders = rootDir.listFiles { folder -> folder.isDirectory }?.map { folder ->
            Folder(folder)
        } ?: emptyList()

        val sortedFolders = folders.sortedBy { it.name }

        // 발견된 폴더들을 리스트에 추가
        items.addAll(sortedFolders)

        adapter.notifyDataSetChanged()
        Log.d("loadFolders", "Final items: ${items.map { it.name }}")
    }

    // 폴더 추가 다이얼로그
    private fun showAddFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edittext, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editText)
        editText.hint = "폴더명을 입력하세요."
        val dialog = AlertDialog.Builder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
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
        if (newFolder.exists()) Toast.makeText(this, "이미 동일한 이름의 폴더가 존재합니다.", Toast.LENGTH_SHORT).show()
        else if (newFolder.mkdir()) {
            items.removeAll { it.name == folderName }
            val newItem = Folder(newFolder)
            items.add(1, newItem)
            adapter.notifyItemInserted(1)
        } else {
            Log.e("addNewFolder", "Failed to create folder")
        }
    }

    // 폴더에 대한 접근 권한을 얻는 함수
    private fun requestFolderAccess(folder: File) {
        val folderUri = Uri.fromFile(folder)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE)
    }

    // onActivityResult()에서 선택된 폴더 Uri를 가져옴
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
            val treeUri = data?.data // 선택된 폴더 Uri

            // 폴더에 대한 접근 권한을 유지
            contentResolver.takePersistableUriPermission(
                treeUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // 압축 로직 실행
            val outputZipFile = File(rootDir, "${DocumentFile.fromTreeUri(this, treeUri)?.name}.zip") // Uri를 File 객체로 변환
            Funbox().compressFolderToZip(this, treeUri, outputZipFile)
            Thread.sleep(100)
            // 압축 파일 공유
            Funbox().shareFile(this@MainActivity, outputZipFile)
            // 공유 후 압축 파일 삭제
            outputZipFile.delete()

        }
    }
}