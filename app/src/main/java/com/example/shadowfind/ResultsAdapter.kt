package com.example.shadowfind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shadowfind.databinding.ItemResultBinding

class ResultsAdapter(private val data: ScanResult) : RecyclerView.Adapter<ResultsAdapter.ViewHolder>() {
    
    private val items = data.links
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val link = items[position]
        holder.bind(link, position + 1)
    }
    
    override fun getItemCount() = items.size
    
    class ViewHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(link: String, position: Int) {
            binding.tvPosition.text = position.toString()
            binding.tvLink.text = link
        }
    }
}
