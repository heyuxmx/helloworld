package com.heyu.zhudeapp.Fragment.countdown

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.databinding.DialogAddAnniversaryBinding

class AddAnniversaryDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use MaterialAlertDialogBuilder for consistent styling with the rest of the app
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val binding = DialogAddAnniversaryBinding.inflate(inflater)

        builder.setView(binding.root)
            .setTitle("添加新的纪念日")
            .setPositiveButton("保存") { _, _ ->
                val name = binding.editTextAnniversaryName.text.toString()
                val day = binding.datePickerAnniversary.dayOfMonth
                val month = binding.datePickerAnniversary.month + 1 // Month is 0-based

                if (name.isNotBlank()) {
                    // Use the Fragment Result API to send data back safely.
                    // This avoids direct listener coupling and is lifecycle-safe.
                    setFragmentResult(REQUEST_KEY, bundleOf(
                        KEY_NAME to name,
                        KEY_MONTH to month,
                        KEY_DAY to day
                    ))
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.cancel()
            }

        return builder.create()
    }

    companion object {
        // Define TAG for showing the dialog
        const val TAG = "AddAnniversaryDialogFragment"
        // Define keys for the request and the data bundle to ensure type safety and consistency.
        const val REQUEST_KEY = "addAnniversaryRequest"
        const val KEY_NAME = "name"
        const val KEY_MONTH = "month"
        const val KEY_DAY = "day"
    }
}
