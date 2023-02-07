package com.exclr8.xen4.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.exclr8.xen4.R
import com.exclr8.xen4.model.UrlsData

class BaseUrlAdapter(
    val context: Context,
    private val urlList: List<UrlsData>
) : RecyclerView.Adapter<BaseUrlAdapter.ViewHolder>() {

    private lateinit var urlListener: OnUrlClick

    interface OnUrlClick {
        fun onUrlClick(position: Int)
    }

    fun setOnUrlClick(listener: OnUrlClick) {
        urlListener = listener
    }

    inner class ViewHolder(itemView: View, listener: OnUrlClick) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(url: UrlsData) {
            val tvUrl = itemView.findViewById<TextView>(R.id.tvUrl)
            tvUrl.text = url.baseUrl
        }

        init {
            itemView.setOnClickListener {
                listener.onUrlClick(position = layoutPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.base_url_back, parent, false)
        return ViewHolder(view, urlListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(urlList[position])
    }

    override fun getItemCount() = urlList.size
}
