package com.heyu.zhudeapp.Fragment.countdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.adapter.CountdownAdapter
import com.heyu.zhudeapp.databinding.FragmentAnniversaryCountdownBinding
import com.heyu.zhudeapp.model.AnniversaryRepository
import com.heyu.zhudeapp.model.CountdownItem
import com.heyu.zhudeapp.model.CustomAnniversary
import com.heyu.zhudeapp.util.LunarCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AnniversaryCountdownFragment : Fragment() {

    private var _binding: FragmentAnniversaryCountdownBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CountdownAdapter
    private lateinit var anniversaryRepository: AnniversaryRepository

    private val builtInAnniversaries = mapOf(
        "见面纪念日" to (12 to 31),
        "在一起的纪念日" to (12 to 2),
        "情人节" to (2 to 14),
        "520" to (5 to 20)
    )
    private val builtInLunarAnniversaries = mapOf(
        "七夕节" to (7 to 7)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnniversaryCountdownBinding.inflate(inflater, container, false)
        anniversaryRepository = AnniversaryRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadAndDisplayAnniversaries()

        binding.fabAddAnniversary.setOnClickListener {
            AddAnniversaryDialogFragment().show(childFragmentManager, AddAnniversaryDialogFragment.TAG)
        }

        setFragmentResultListener(AddAnniversaryDialogFragment.REQUEST_KEY) { _, bundle ->
            val name = bundle.getString(AddAnniversaryDialogFragment.KEY_NAME)
            val month = bundle.getInt(AddAnniversaryDialogFragment.KEY_MONTH)
            val day = bundle.getInt(AddAnniversaryDialogFragment.KEY_DAY)
            if (name != null && month != 0 && day != 0) {
                addNewAnniversary(name, month, day)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CountdownAdapter(mutableListOf())
        binding.anniversaryRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.anniversaryRecyclerView.adapter = adapter

        adapter.onItemLongClickListener = {
            showDeleteConfirmationDialog(it)
        }
    }

    private fun loadAndDisplayAnniversaries() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()

            // 1. Calculate built-in Gregorian anniversaries
            val calculatedGregorian = builtInAnniversaries.map { (name, date) ->
                val (month, day) = date
                val nextDate = getNextGregorianDate(today, month, day)
                createCountdownItem(name, nextDate, today, isDeletable = false)
            }

            // 2. Calculate built-in Lunar anniversaries
            val calculatedLunar = builtInLunarAnniversaries.map { (name, date) ->
                val (month, day) = date
                val nextDate = LunarCalendar.getLunarDate(month, day)
                createCountdownItem(name, nextDate, today, isDeletable = false)
            }

            // 3. Load custom anniversaries and calculate them
            val builtInNames = builtInAnniversaries.keys + builtInLunarAnniversaries.keys
            val customAnniversaries = anniversaryRepository.getAnniversaries().filter { it.name !in builtInNames }

            val calculatedCustoms = customAnniversaries.map { item ->
                val nextDate = getNextGregorianDate(today, item.month, item.day)
                createCountdownItem(item.name, nextDate, today, isDeletable = true)
            }

            // 4. Combine and sort
            val combinedList = (calculatedGregorian + calculatedLunar + calculatedCustoms).sortedBy { it.daysRemaining }

            withContext(Dispatchers.Main) {
                adapter.updateList(combinedList)
            }
        }
    }

    private fun getNextGregorianDate(today: LocalDate, month: Int, day: Int): LocalDate {
        var date = LocalDate.of(today.year, month, day)
        if (date.isBefore(today)) {
            date = date.plusYears(1)
        }
        return date
    }

    private fun createCountdownItem(name: String, nextEventDate: LocalDate, today: LocalDate, isDeletable: Boolean): CountdownItem {
        val daysRemaining = ChronoUnit.DAYS.between(today, nextEventDate)

        val displayName: String
        val yearToDisplay: Int

        if (name == "在一起的纪念日" || name == "见面纪念日") {
            val n = nextEventDate.year - 2024
            yearToDisplay = 2024
            displayName = if (name == "在一起的纪念日") {
                if (n > 0) "第${n}个在一起的纪念日" else name
            } else { // "见面纪念日"
                if (n > 0) "第${n}个见面纪念日" else name
            }
        } else {
            displayName = name
            yearToDisplay = nextEventDate.year
        }

        val dateOverride = String.format("%d年%d月%d日", yearToDisplay, nextEventDate.monthValue, nextEventDate.dayOfMonth)

        return CountdownItem(
            name = displayName,
            month = nextEventDate.monthValue,
            day = nextEventDate.dayOfMonth,
            dateOverride = dateOverride,
            daysRemaining = daysRemaining,
            isDeletable = isDeletable
        )
    }

    private fun addNewAnniversary(name: String, month: Int, day: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val builtInNames = builtInAnniversaries.keys + builtInLunarAnniversaries.keys
            if (name in builtInNames) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "这是一个内置纪念日，无法重复添加", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val currentCustomAnniversaries: MutableList<CustomAnniversary> = anniversaryRepository.getAnniversaries().toMutableList()
            if (currentCustomAnniversaries.any { it.name == name }) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "已存在同名纪念日", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val newItem = CustomAnniversary(name, month, day)
            currentCustomAnniversaries.add(newItem)
            anniversaryRepository.saveAnniversaries(currentCustomAnniversaries)

            withContext(Dispatchers.Main) {
                loadAndDisplayAnniversaries()
            }
        }
    }

    private fun showDeleteConfirmationDialog(item: CountdownItem) {
        if (!item.isDeletable) {
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("删除纪念日")
            .setMessage("您确定要删除 ''${item.name}'' 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteAnniversary(item)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteAnniversary(itemToDelete: CountdownItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentCustomAnniversaries: MutableList<CustomAnniversary> = anniversaryRepository.getAnniversaries().toMutableList()
            val originalName = if (itemToDelete.name.startsWith("第")) {
                if (itemToDelete.name.contains("在一起的纪念日")) "在一起的纪念日" else "见面纪念日"
            } else {
                itemToDelete.name
            }

            val removed = currentCustomAnniversaries.removeAll { it.name == originalName }
            if (removed) {
                anniversaryRepository.saveAnniversaries(currentCustomAnniversaries)
            }
            withContext(Dispatchers.Main) {
                loadAndDisplayAnniversaries()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
