package com.heyu.zhudeapp.Fragment.countdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.adapter.CountdownAdapter
import com.heyu.zhudeapp.databinding.FragmentAnniversaryCountdownBinding
import com.heyu.zhudeapp.model.CountdownItem
import com.heyu.zhudeapp.repository.AnniversaryRepository

class AnniversaryCountdownFragment : Fragment(), AddAnniversaryDialogFragment.AddAnniversaryDialogListener {

    private var _binding: FragmentAnniversaryCountdownBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AnniversaryRepository
    private lateinit var anniversaryList: MutableList<CountdownItem>
    private lateinit var adapter: CountdownAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnniversaryCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = AnniversaryRepository(requireContext())
        anniversaryList = repository.getAnniversaries()

        adapter = CountdownAdapter(anniversaryList)
        binding.anniversaryRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.anniversaryRecyclerView.adapter = adapter

        binding.fabAddAnniversary.setOnClickListener {
            val dialog = AddAnniversaryDialogFragment()
            dialog.show(childFragmentManager, "AddAnniversaryDialogFragment")
        }
    }

    override fun onAnniversaryAdded(name: String, month: Int, day: Int) {
        val newItem = CountdownItem(name = name, month = month, day = day)
        
        // Add to adapter for immediate UI update
        adapter.addItem(newItem)
        
        // Save the entire updated list to repository
        repository.saveAnniversaries(anniversaryList) 
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
