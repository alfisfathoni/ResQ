package com.example.resq.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.resq.R
import com.example.resq.ui.model.EmergencyContact

/**
 * Custom RecyclerView.Adapter & ViewHolder for rendering Emergency Contacts list
 * in ProfileScreen matching the UI design specs.
 */
class EmergencyContactAdapter(
    private var contacts: List<EmergencyContact> = emptyList(),
    private val onCallClick: (EmergencyContact) -> Unit = {},
) : RecyclerView.Adapter<EmergencyContactAdapter.EmergencyContactViewHolder>() {

    fun updateContacts(newContacts: List<EmergencyContact>) {
        this.contacts = newContacts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return EmergencyContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmergencyContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact, position + 1, onCallClick)
    }

    override fun getItemCount(): Int = contacts.size

    class EmergencyContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_contact_number)
        private val tvName: TextView = itemView.findViewById(R.id.tv_contact_name)
        private val tvPhone: TextView = itemView.findViewById(R.id.tv_contact_phone)
        private val btnCall: ImageView = itemView.findViewById(R.id.btn_call_contact)

        fun bind(
            contact: EmergencyContact,
            displayIndex: Int,
            onCallClick: (EmergencyContact) -> Unit,
        ) {
            tvNumber.text = displayIndex.toString()
            tvName.text = contact.name
            tvPhone.text = contact.phone

            btnCall.setOnClickListener {
                onCallClick(contact)
            }
        }
    }
}
