package com.example.status_saver_app
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import com.example.status_saver_app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var settingsList: Array<String>
    private lateinit var settingsIconList: Array<Int>
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)
        supportActionBar?.setTitle(R.string.sett_name)
        listView = binding.settingsList

        settingsList = arrayOf(Constant.SH_ARE, Constant.R_ATE)
        settingsIconList = arrayOf(R.drawable.share, R.drawable.star_svgrepo_com)

        val adapter = SettingsListAdapter(this, settingsList, settingsIconList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> shareApp()
                1 -> showRatingDialog()
            }
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            val shareSubject = "Check out this awesome app!"
            var shareMessage = "I found this amazing app. Give it a try!\n\n"
            shareMessage += "https://play.google.com/store/apps/details?id=${packageName}"
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun showRatingDialog() {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_rating)
        }

        val ratingBar = dialog.findViewById<RatingBar>(R.id.rating_bar).apply {
            numStars = 5
            stepSize = 1.0f
        }

        val submitButton = dialog.findViewById<Button>(R.id.submit_button)
        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            submitRatingToPlayStore(rating.toString())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitRatingToPlayStore(rating: String) {
        val packageName = packageName
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
    }
}
