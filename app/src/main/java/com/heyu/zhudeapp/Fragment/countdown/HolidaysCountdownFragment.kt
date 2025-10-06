package com.heyu.zhudeapp.Fragment.countdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.adapter.CountdownAdapter
import com.heyu.zhudeapp.databinding.FragmentHolidaysCountdownBinding
import com.heyu.zhudeapp.model.AnniversaryRepository
import com.heyu.zhudeapp.model.CountdownItem
import com.heyu.zhudeapp.model.CustomAnniversary
import com.heyu.zhudeapp.util.LunarCalendar.getLunarDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HolidaysCountdownFragment : Fragment() {

    private var _binding: FragmentHolidaysCountdownBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AnniversaryRepository
    private lateinit var adapter: CountdownAdapter

    private val lunarHolidays = mapOf(
        "春节" to (1 to 1),
        "端午节" to (5 to 5),
        "中秋节" to (8 to 15)
    )

    private val gregorianHolidays = mapOf(
        "元旦" to (1 to 1),
        "清明节" to (4 to 5),
        "劳动节" to (5 to 1),
        "国庆节" to (10 to 1)
    )

    private val builtInHolidayNames by lazy { lunarHolidays.keys + gregorianHolidays.keys }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidaysCountdownBinding.inflate(inflater, container, false)
        repository = AnniversaryRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadAndDisplayData()

        binding.fabAddAnniversary.setOnClickListener {
            showAddAnniversaryDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = CountdownAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        adapter.onItemLongClickListener = {
            showDeleteConfirmationDialog(it)
        }
    }

    private fun loadAndDisplayData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()

            // 1. Calculate built-in holidays (always recurring)
            val calculatedBuiltIns = (lunarHolidays.keys + gregorianHolidays.keys).map {
                val nextDate = if (lunarHolidays.containsKey(it)) {
                    val (lunarMonth, lunarDay) = lunarHolidays[it]!!
                    getLunarDate(lunarMonth, lunarDay)
                } else {
                    val (month, day) = gregorianHolidays[it]!!
                    getNextGregorianDate(today, month, day)
                }
                createCountdownItem(it, nextDate, today, isDeletable = false)
            }

            // 2. Load and calculate custom holidays from the unified repository
            val customHolidays = repository.getAnniversaries()
                .filter { it.name !in builtInHolidayNames } // Filter out duplicates
                .mapNotNull { anniversary ->
                    val nextDate: LocalDate
                    if (anniversary.year != null) {
                        // This is a one-time event
                        nextDate = LocalDate.of(anniversary.year, anniversary.month, anniversary.day)
                        if (nextDate.isBefore(today)) {
                            return@mapNotNull null // Filter out past one-time events
                        }
                    } else {
                        // This is a recurring event
                        nextDate = if (anniversary.isLunar) {
                            getLunarDate(anniversary.month, anniversary.day)
                        } else {
                            getNextGregorianDate(today, anniversary.month, anniversary.day)
                        }
                    }
                    createCountdownItem(anniversary.name, nextDate, today, isDeletable = true)
                }

            // 3. Combine and sort
            val combinedList = (calculatedBuiltIns + customHolidays).sortedBy { it.daysRemaining }

            withContext(Dispatchers.Main) {
                adapter.updateList(combinedList)
            }
        }
    }

    private fun showAddAnniversaryDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_anniversary, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_text_anniversary_name)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.date_picker_anniversary)
        val lunarSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switch_lunar)


        MaterialAlertDialogBuilder(requireContext())
            .setTitle("添加新节日")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = nameEditText.text.toString()

                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (name in builtInHolidayNames) {
                    Toast.makeText(requireContext(), "这是一个内置节日，无法重复添加", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedYear = datePicker.year
                // Heuristic: If the selected year is not the current year, treat it as a one-time event.
                val eventYear = if (selectedYear != LocalDate.now().year) selectedYear else null

                val newAnniversary = CustomAnniversary(
                    name = name,
                    month = datePicker.month + 1, // DatePicker month is 0-indexed
                    day = datePicker.dayOfMonth,
                    year = eventYear,
                    isLunar = lunarSwitch.isChecked
                )
                addNewHoliday(newAnniversary)
            }
            .show()
    }

    private fun addNewHoliday(newAnniversary: CustomAnniversary) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentList = repository.getAnniversaries()
            if (currentList.any { it.name == newAnniversary.name }) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "已存在同名节日", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val newList = currentList + newAnniversary
            repository.saveAnniversaries(newList)

            withContext(Dispatchers.Main) {
                loadAndDisplayData()
            }
        }
    }

    private fun showDeleteConfirmationDialog(item: CountdownItem) {
        if (!item.isDeletable) {
            return // Do not show dialog for non-deletable items
        }
        AlertDialog.Builder(requireContext())
            .setTitle("删除节日")
            .setMessage("您确定要删除 ''${item.name}'' 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteHoliday(item)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteHoliday(itemToDelete: CountdownItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentList = repository.getAnniversaries()
            val newList = currentList.filter { it.name != itemToDelete.name }

            if (newList.size < currentList.size) {
                repository.saveAnniversaries(newList)
                withContext(Dispatchers.Main) {
                    loadAndDisplayData()
                }
            }
        }
    }


    private fun getNextGregorianDate(today: LocalDate, month: Int, day: Int): LocalDate {
        val dateInCurrentYear = LocalDate.of(today.year, month, day)
        return if (dateInCurrentYear.isBefore(today)) {
            dateInCurrentYear.plusYears(1)
        } else {
            dateInCurrentYear
        }
    }

    private fun createCountdownItem(name: String, nextEventDate: LocalDate, today: LocalDate, isDeletable: Boolean): CountdownItem {
        val daysRemaining = ChronoUnit.DAYS.between(today, nextEventDate)
        val dateOverride = String.format("%d年%d月%d日", nextEventDate.year, nextEventDate.monthValue, nextEventDate.dayOfMonth)

        return CountdownItem(
            name = name,
            month = nextEventDate.monthValue,
            day = nextEventDate.dayOfMonth,
            dateOverride = dateOverride,
            daysRemaining = daysRemaining,
            isDeletable = isDeletable
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
