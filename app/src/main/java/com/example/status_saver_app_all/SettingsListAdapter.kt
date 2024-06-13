package com.example.status_saver_app_all

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.status_saver_app_all.databinding.SettingsListItemBinding

class SettingsListAdapter(context: Context, private val settingsList: Array<String>, private val settingsIconList: Array<Int>) :
    ArrayAdapter<String>(context, R.layout.settings_list_item, settingsList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: SettingsListItemBinding
        var convertView = convertView

        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            binding = SettingsListItemBinding.inflate(inflater, parent, false)
            convertView = binding.root
            convertView.tag = binding
        } else {
            binding = convertView.tag as SettingsListItemBinding
        }

        binding.settingsTextView.text = settingsList[position]
        binding.settingsIconView.setImageResource(settingsIconList[position])

        return convertView
    }
}
