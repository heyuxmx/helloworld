package com.heyu.zhudeapp.Fragment.countdown

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.heyu.zhudeapp.databinding.DialogAddAnniversaryBinding
import java.util.Calendar

class AddAnniversaryDialogFragment : DialogFragment() {

    interface AddAnniversaryDialogListener {
        fun onAnniversaryAdded(name: String, month: Int, day: Int)
    }

    private var listener: AddAnniversaryDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Try to set the listener from the parent Fragment
        listener = parentFragment as? AddAnniversaryDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val binding = DialogAddAnniversaryBinding.inflate(inflater)

            builder.setView(binding.root)
                .setTitle("添加新的纪念日")
                .setPositiveButton("保存") { _, _ ->
                    val name = binding.editTextAnniversaryName.text.toString()
                    val day = binding.datePickerAnniversary.dayOfMonth
                    val month = binding.datePickerAnniversary.month + 1 // Month is 0-based

                    if (name.isNotBlank()) {
                        listener?.onAnniversaryAdded(name, month, day)
                    }
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
