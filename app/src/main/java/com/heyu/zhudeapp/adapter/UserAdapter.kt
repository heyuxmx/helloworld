package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.UserProfile

/**
 * Adapter for the user list RecyclerView.
 * This adapter takes a list of UserProfile objects and binds them to the UI.
 */
class UserAdapter(
    private var users: List<UserProfile>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    /**
     * ViewHolder for a single user item.
     * It holds references to the UI views that will be populated with user data.
     */
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImageView: ImageView = itemView.findViewById(R.id.user_avatar_image)
        private val usernameTextView: TextView = itemView.findViewById(R.id.user_username_text)

        /**
         * Binds a UserProfile object to the views.
         * It uses Glide to load the user's avatar and sets the username text.
         */
        fun bind(user: UserProfile) {
            usernameTextView.text = user.username

            Glide.with(itemView.context)
                .load(user.avatarUrl)
                .placeholder(R.drawable.hollowlike) // A placeholder while the image is loading
                .error(R.drawable.hollowlike) // An error image if loading fails
                .circleCrop() // Make the avatar circular
                .into(avatarImageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    /**
     * Updates the list of users in the adapter and notifies the RecyclerView to refresh.
     * The @SuppressLint("NotifyDataSetChanged") is used because we are replacing the entire list.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateUsers(newUsers: List<UserProfile>) {
        this.users = newUsers
        notifyDataSetChanged() // This is a simple way to refresh, for more complex apps DiffUtil is better
    }
}
