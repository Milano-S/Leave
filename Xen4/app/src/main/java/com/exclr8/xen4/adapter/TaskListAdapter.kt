package com.exclr8.xen4.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.exclr8.xen4.R
import com.exclr8.xen4.model.Task

class TaskListAdapter(
    val context: Context,
    private val taskList: List<Task>
) : RecyclerView.Adapter<TaskListAdapter.ViewHolder>() {

    private lateinit var taskListener: OnTaskClick

    interface OnTaskClick {
        fun onTaskClick(position: Int)
    }

    fun setOnTaskClick(listener: OnTaskClick) {
        taskListener = listener
    }

    inner class ViewHolder(itemView: View, listener: OnTaskClick) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(task: Task) {
            val tvHeader = itemView.findViewById<TextView>(R.id.tvHeader)
            val tvProcessName = itemView.findViewById<TextView>(R.id.tvProcessName)
            val tvEmployeeName = itemView.findViewById<TextView>(R.id.tvEmployeeName)
            val tvOpenAge = itemView.findViewById<TextView>(R.id.tvOpenAge)
            val tvProgress = itemView.findViewById<TextView>(R.id.tvProgress)
            val ivClock = itemView.findViewById<ImageView>(R.id.ivClock)

            tvHeader.text = task.Header
            tvEmployeeName.text = task.EmployeeName
            tvProcessName.text = task.LeaveType

            if (task.IsOpen){
                tvOpenAge.text = task.OpenAge
            }else{
                tvOpenAge.text = ""
                tvProgress.isVisible = false
                ivClock.isVisible = false
            }
        }

        init {
            itemView.setOnClickListener {
                listener.onTaskClick(position = layoutPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false)
        return ViewHolder(view, taskListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(taskList[position])
    }

    override fun getItemCount() = taskList.size
}