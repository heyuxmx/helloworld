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
import com.heyu.zhudeapp.databinding.FragmentBirthdayCountdownBinding
import com.heyu.zhudeapp.model.AnniversaryRepository
import com.heyu.zhudeapp.model.CountdownItem
import com.heyu.zhudeapp.model.CustomAnniversary
import com.heyu.zhudeapp.util.LunarCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BirthdayCountdownFragment : Fragment() {

    private var _binding: FragmentBirthdayCountdownBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CountdownAdapter
    private lateinit var repository: AnniversaryRepository

    private val builtInBirthdays = mapOf(
        "小高的生日" to (10 to 27),
        "小徐的生日" to (2 to 11)
    )
    private val builtInNames by lazy { builtInBirthdays.keys }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBirthdayCountdownBinding.inflate(inflater, container, false)
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

            val calculatedBuiltIns = builtInBirthdays.map { (name, date) ->
                val (month, day) = date
                val nextDate = getNextGregorianDate(today, month, day)
                createCountdownItem(name, nextDate, today, isDeletable = false) // Built-ins are not deletable
            }

            val customItems = repository.getAnniversaries()
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

            val combinedList = (calculatedBuiltIns + customItems).sortedBy { it.daysRemaining }

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
            .setTitle("添加新生日")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = nameEditText.text.toString()

                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (name in builtInNames) {
                    Toast.makeText(requireContext(), "这是一个内置生日，无法重复添加", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedYear = datePicker.year
                val eventYear = if (selectedYear != LocalDate.now().year) selectedYear else null

                val newBirthday = CustomAnniversary(
                    name = name,
                    month = datePicker.month + 1,
                    day = datePicker.dayOfMonth,
                    year = eventYear,
                    isLunar = lunarSwitch.isChecked
                )
                addNewItem(newBirthday)
            }
            .show()
    }

    private fun addNewItem(newItem: CustomAnniversary) {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentList = repository.getAnniversaries()
            if (currentList.any { it.name == newItem.name }) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "已存在同名项目", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val newList = currentList + newItem
            repository.saveAnniversaries(newList)

            withContext(Dispatchers.Main) {
                loadAndDisplayData()
            }
        }
    }

    private fun showDeleteConfirmationDialog(item: CountdownItem) {
        if (!item.isDeletable) {
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("删除生日")
            .setMessage("您确定要删除 ''${item.name}'' 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteItem(itemToDelete: CountdownItem) {
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
