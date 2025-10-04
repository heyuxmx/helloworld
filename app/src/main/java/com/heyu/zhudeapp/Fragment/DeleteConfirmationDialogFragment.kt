package com.heyu.zhudeapp.Fragment

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.heyu.zhudeapp.data.Post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DeleteConfirmationDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val postJson = arguments?.getString(BUNDLE_KEY_POST)

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("你确定要删除这条动态吗？")
                .setPositiveButton("删除") { _, _ ->
                    postJson?.let {
                        val resultBundle = Bundle().apply {
                            putBoolean(BUNDLE_KEY_CONFIRMED, true)
                            putString(BUNDLE_KEY_POST, it)
                        }
                        parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
                    }
                }
                .setNegativeButton("取消") { _, _ ->
                    // User cancelled the dialog
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val REQUEST_KEY = "DELETE_REQUEST"
        const val BUNDLE_KEY_CONFIRMED = "DELETE_CONFIRMED"
        const val BUNDLE_KEY_POST = "POST_TO_DELETE"

        fun newInstance(post: Post): DeleteConfirmationDialogFragment {
            val fragment = DeleteConfirmationDialogFragment()
            val args = Bundle().apply {
                putString(BUNDLE_KEY_POST, Json.encodeToString(post))
            }
            fragment.arguments = args
            return fragment
        }
    }
}
