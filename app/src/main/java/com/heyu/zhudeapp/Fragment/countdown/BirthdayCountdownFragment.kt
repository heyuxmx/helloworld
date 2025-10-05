package com.heyu.zhudeapp.Fragment.countdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.adapter.CountdownAdapter
import com.heyu.zhudeapp.databinding.FragmentBirthdayCountdownBinding
import com.heyu.zhudeapp.model.CountdownItem

class BirthdayCountdownFragment : Fragment() {

    private var _binding: FragmentBirthdayCountdownBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBirthdayCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val birthdayList = mutableListOf(
            CountdownItem(name = "小高的生日", month = 10, day = 27),
            CountdownItem(name = "小徐的生日", month = 2, day = 11)
        )

        val adapter = CountdownAdapter(birthdayList)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
