package com.exclr8.xen4.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.exclr8.xen4.R
import com.exclr8.xen4.adapter.TaskListAdapter
import com.exclr8.xen4.api.TasksInterface
import com.exclr8.xen4.databinding.FragmentTaskBinding
import com.exclr8.xen4.model.Task
import com.exclr8.xen4.model.TaskListResponse
import com.exclr8.xen4.model.UserTasksRequest
import com.exclr8.xen4.sharedPref.SharedPreference
import com.exclr8.xen4.viewModel.ViewModelXen
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "TaskFragment"

class TaskFragment : Fragment(), SwipyRefreshLayout.OnRefreshListener {

    private lateinit var binding: FragmentTaskBinding
    private lateinit var adapter: TaskListAdapter
    private val vm: ViewModelXen by activityViewModels()
    private val baseUrl: String by lazy {
        SharedPreference(requireContext()).getValueString("baseUrl").toString()
    }
    private val taskList: List<Task> by lazy { vm.taskList }
    private var take = 10
    private lateinit var fragContext: Context
    //private val rvTaskList : RecyclerView by lazy { requireView().findViewById(R.id.rvTaskList) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parentActivity = (activity as AppCompatActivity)
        val dl = parentActivity.findViewById<DrawerLayout>(R.id.dlMain)
        dl?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        val actionBar = parentActivity.supportActionBar
        actionBar?.apply {
            hide()
            title = ""
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTaskBinding.bind(view)
        fragContext = requireContext()


        runBlocking {
            getUserTasks(0, take, fragContext)
        }

        binding.miBurger.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        //val taskList = mutableListOf<Task>()
        //repeat(5) { taskList.addAll(mutableListOf(Task("", "", "Employee Name", "Leave Request", 0, true, "", "2 minutes", "", "HR Review",))) }

        adapter = TaskListAdapter(requireContext(), taskList)
        binding.rvTaskList.adapter = adapter
        binding.rvTaskList.layoutManager = GridLayoutManager(requireContext(), 2)
        initLayout()
        if (taskList.isEmpty()) {
            binding.apply {
                rvTaskList.isVisible = false
                llNoTasks.isVisible = true
            }
        }
        adapter.setOnTaskClick(object : TaskListAdapter.OnTaskClick {
            override fun onTaskClick(position: Int) {
                val task = taskList[position]
                loadTaskWebView(task.InstanceId.toString(), task.ActivityGuid, task)
            }
        })
    }

    override fun onRefresh(direction: SwipyRefreshLayoutDirection?) {
        Log.d(
            "TaskFragment",
            "Refresh triggered at " + if (direction === SwipyRefreshLayoutDirection.TOP) "top" else "bottom"
        )
        Log.d("TaskFragment", "TaskListCount : " + vm.taskList.size.toString())

        take += 10
        getUserTasks(0, take, requireContext())

        binding.rvTaskList.scheduleLayoutAnimation()

    }

    private fun initLayout() {
        binding.rvTaskList.adapter = adapter
        binding.swipyrefreshlayout.setOnRefreshListener(this)
    }

    private fun loadTaskWebView(instanceId: String, activityGuid: String, task: Task) {
        //val taskUrl = "CUI/TaskList/OpenWorkItemByInstance?instanceId=$instanceId&activityGuid=$activityGuid"
        val taskUrl = task.Url
        vm.currentPageUrl = taskUrl
        findNavController().navigate(R.id.action_taskFragment_to_webViewFragment)
    }

    private fun getUserTasks(skip: Int, take: Int, context: Context) {

        val userTasksRequest = UserTasksRequest(skip, take)
        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(SharedPreference(context).getValueString("baseUrl").toString())
            .build()
            .create(TasksInterface::class.java)

        val retrofitData = retrofitBuilder.getUserTasks(
            userTasksRequest,
            SharedPreference(context).getValueString("userKey").toString()
        )
        retrofitData.enqueue(object : Callback<TaskListResponse> {
            override fun onResponse(
                call: Call<TaskListResponse>,
                response: Response<TaskListResponse>
            ) {
                if (response.body() != null) {
                    response.body()!!.Tasks.forEach { task ->
                        Log.i(TAG, task.Url)
                    }
                    vm.taskList.clear()
                    vm.setTaskList(response.body()!!)
                    adapter = TaskListAdapter(fragContext, taskList)
                    if (taskList.isEmpty()) {
                        binding.apply {
                            rvTaskList.isVisible = false
                            llNoTasks.isVisible = true
                        }
                    }
                    binding.rvTaskList.adapter = adapter
                    binding.rvTaskList.layoutManager = GridLayoutManager(requireContext(), 2)
                    initLayout()
                    adapter.setOnTaskClick(object : TaskListAdapter.OnTaskClick {
                        override fun onTaskClick(position: Int) {
                            val task = taskList[position]
                            loadTaskWebView(task.InstanceId.toString(), task.ActivityGuid, task)
                        }
                    })
                    binding.rvTaskList.scrollToPosition(vm.taskList.size - 1)
                    binding.swipyrefreshlayout.isRefreshing = false
                }
            }

            override fun onFailure(call: Call<TaskListResponse>, t: Throwable) {
                Log.i(TAG, t.message.toString())
            }
        })
    }

    private fun initializeEmptyTaskList() {
        (taskList as MutableList).clear()
        adapter = TaskListAdapter(fragContext, taskList)
        if (taskList.isEmpty()) {
            binding.apply {
                rvTaskList.isVisible = false
                llNoTasks.isVisible = true
            }
        }
        binding.rvTaskList.adapter = adapter
        binding.rvTaskList.layoutManager = GridLayoutManager(requireContext(), 2)
        initLayout()
        adapter.setOnTaskClick(object : TaskListAdapter.OnTaskClick {
            override fun onTaskClick(position: Int) {
                val task = taskList[position]
                loadTaskWebView(task.InstanceId.toString(), task.ActivityGuid, task)
            }
        })
        binding.rvTaskList.scrollToPosition(vm.taskList.size - 1)
        binding.swipyrefreshlayout.isRefreshing = false
    }
}