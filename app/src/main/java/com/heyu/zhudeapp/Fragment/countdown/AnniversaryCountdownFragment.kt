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
    private lateinit var repository: AnniversaryRepository
    private lateinit var adapter: CountdownAdapter

    private val builtInAnniversaries = mapOf(
        "见面纪念日" to (12 to 31),
        "在一起的纪念日" to (12 to 2),
        "情人节" to (2 to 14),
        "520" to (5 to 20)
    )
    private val builtInLunarAnniversaries = mapOf(
        "七夕节" to (7 to 7)
    )
    private val builtInNames by lazy { builtInAnniversaries.keys + builtInLunarAnniversaries.keys }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnniversaryCountdownBinding.inflate(inflater, container, false)
        repository = AnniversaryRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadAndDisplayAnniversaries()

        binding.fabAddAnniversary.setOnClickListener {
            showAddAnniversaryDialog()
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

            val calculatedBuiltIns = (builtInAnniversaries.keys + builtInLunarAnniversaries.keys).map {
                val nextDate = if (builtInLunarAnniversaries.containsKey(it)) {
                    val (lunarMonth, lunarDay) = builtInLunarAnniversaries[it]!!
                    LunarCalendar.getLunarDate(lunarMonth, lunarDay)
                } else {
                    val (month, day) = builtInAnniversaries[it]!!
                    getNextGregorianDate(today, month, day)
                }
                createCountdownItem(it, nextDate, today, isDeletable = false)
            }

            val customHolidays = repository.getAnniversaries()
                .filter { it.name !in builtInNames } 
                .mapNotNull { anniversary ->
                    val nextDate: LocalDate
                    if (anniversary.year != null) {
                        nextDate = LocalDate.of(anniversary.year, anniversary.month, anniversary.day)
                        if (nextDate.isBefore(today)) {
                            return@mapNotNull null
                        }
                    } else {
                        nextDate = if (anniversary.isLunar) {
                            LunarCalendar.getLunarDate(anniversary.month, anniversary.day)
                        } else {
                            getNextGregorianDate(today, anniversary.month, anniversary.day)
                        }
                    }
                    createCountdownItem(anniversary.name, nextDate, today, isDeletable = true)
                }

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
            .setTitle("添加新纪念日")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = nameEditText.text.toString()

                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (name in builtInNames) {
                    Toast.makeText(requireContext(), "这是一个内置纪念日，无法重复添加", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedYear = datePicker.year
                val eventYear = if (selectedYear != LocalDate.now().year) selectedYear else null

                val newAnniversary = CustomAnniversary(
                    name = name,
                    month = datePicker.month + 1,
                    day = datePicker.dayOfMonth,
                    year = eventYear,
                    isLunar = lunarSwitch.isChecked
                )
                addNewAnniversary(newAnniversary)
            }
            .show()
    }

    private fun addNewAnniversary(newAnniversary: CustomAnniversary) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentList = repository.getAnniversaries()
            if (currentList.any { it.name == newAnniversary.name }) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "已存在同名纪念日", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val newList = currentList + newAnniversary
            repository.saveAnniversaries(newList)

            withContext(Dispatchers.Main) {
                loadAndDisplayAnniversaries()
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
            displayName = if (n > 0) "第${n}个${name}" else name
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

    private fun showDeleteConfirmationDialog(item: CountdownItem) {
        if (!item.isDeletable) {
            Toast.makeText(requireContext(), "内置纪念日不可删除", Toast.LENGTH_SHORT).show()
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
            val currentList = repository.getAnniversaries()
            
            // Handle special "第n个" names
            val originalName = if (itemToDelete.name.startsWith("第")) {
                if (itemToDelete.name.contains("在一起的纪念日")) "在一起的纪念日" 
                else if (itemToDelete.name.contains("见面纪念日")) "见面纪念日"
                else itemToDelete.name
            } else {
                itemToDelete.name
            }

            val newList = currentList.filter { it.name != originalName }

            if (newList.size < currentList.size) {
                repository.saveAnniversaries(newList)
            }
             // Also need to handle deleting built-in items if they are now deletable
            // This part of logic needs to be carefully crafted.
            // For now, we only handle custom anniversaries.
            
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
