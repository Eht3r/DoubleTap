package com.example.doubletap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import  androidx.recyclerview.widget.RecyclerView
import com.example.doubletap.databinding.FolderBinding // folder.xml

class MainAdapter(
    val items: MutableList<Folder>,
    private val itemClickListener: (Folder) -> Unit
) : RecyclerView.Adapter<MainAdapter.ViewHolder>() {

    class ViewHolder(val binding: FolderBinding) : RecyclerView.ViewHolder(binding.root)

    // 뷰홀더 생성
    // 뷰 홀더는 RecyclerView의 아이템 뷰를 보유하는 객체
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    // 뷰홀더에 데이터를 바인딩
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.folderName.text = item.name

        if (item.name == "폴더 추가") {
            holder.binding.fileCount.visibility = ViewGroup.GONE
        } else {
            holder.binding.fileCount.visibility = ViewGroup.VISIBLE
            holder.binding.fileCount.text = "${item.fileCount ?: 0}개의 파일"
        }
        holder.itemView.setOnClickListener {
            itemClickListener(item)
        }
    }

    // 아이템의 개수 반환
    override fun getItemCount(): Int = items.size

    // 아이템 삭제
    fun deleteItem(position: Int) {
        val item = items[position]
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
        item.folder.deleteRecursively()
    }
}