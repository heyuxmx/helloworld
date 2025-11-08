package com.heyu.zhudeapp.Fragment.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.databinding.FragmentUserManagementBinding
import com.heyu.zhudeapp.viewmodel.UserManagementViewModel

/**
 * A Fragment that displays the profile of the SINGLE current user,
 * determined by the application's build flavor (e.g., xiaogao or xiaoxu).
 */
class UserManagementFragment : Fragment() {

    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    // Lazily initialize the ViewModel using the by viewModels() KTX delegate.
    private val viewModel: UserManagementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()

        // Initial fetch of the current user when the view is created.
        viewModel.fetchCurrentUser()
    }

    /**
     * Observes the LiveData from the ViewModel.
     * Updates the UI with the current user's profile information.
     * Shows a toast message if an error occurs.
     */
    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Assuming your binding has an imageView 'userAvatarImage' and a textView 'userNameText'
                // You will need to add these views to your fragment_user_management.xml file.
                binding.userNameText.text = user.username
                Glide.with(this)
                    .load(user.avatarUrl)
                    .circleCrop() // Optional: Makes the avatar image circular
                    .into(binding.userAvatarImage)
            } else {
                // Handle the case where the user is not found or an error occurred
                binding.userNameText.text = "User not available"
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            // If there's an error, show a toast to the user.
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}