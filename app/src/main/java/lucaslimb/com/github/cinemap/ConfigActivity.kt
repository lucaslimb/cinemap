package lucaslimb.com.github.cinemap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import lucaslimb.com.github.cinemap.utils.Constants

class ConfigActivity : AppCompatActivity() {

    private lateinit var rgLangGroup: RadioGroup
    private lateinit var rgSavedGroup: RadioGroup
    private lateinit var rgModeGroup: RadioGroup
    private lateinit var rgYearGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val btnBack: ImageButton = findViewById(R.id.toolbar_config_btn_back)
        btnBack.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        rgLangGroup = findViewById(R.id.rg_lang_group)
        rgSavedGroup = findViewById(R.id.rg_saved_group)
        rgModeGroup = findViewById(R.id.rg_mode_group)
        rgYearGroup = findViewById(R.id.rg_year_group)

        loadSavedPreferences()

        rgLangGroup.setOnCheckedChangeListener { group, checkedId ->
            savePreference(Constants.PREF_KEY_LANG_SELECTION, checkedId)
        }
        rgSavedGroup.setOnCheckedChangeListener { group, checkedId ->
            savePreference(Constants.PREF_KEY_SAVED_SELECTION, checkedId)
        }

//        rgModeGroup.setOnCheckedChangeListener { group, checkedId ->
//            savePreference(Constants.PREF_KEY_MODE_SELECTION, checkedId)
//        }
//
//        rgYearGroup.setOnCheckedChangeListener { group, checkedId ->
//            savePreference(Constants.PREF_KEY_YEAR_SELECTION, checkedId)
//        }

    }

    private fun loadSavedPreferences() {
        val sharedPrefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        val langCheckedId = sharedPrefs.getInt(Constants.PREF_KEY_LANG_SELECTION, R.id.cb_config_lang_original)
        rgLangGroup.check(langCheckedId)

        val savedCheckedId = sharedPrefs.getInt(Constants.PREF_KEY_SAVED_SELECTION, R.id.cb_config_saved_no)
        rgSavedGroup.check(savedCheckedId)

//        val modeCheckedId = sharedPrefs.getInt(Constants.PREF_KEY_MODE_SELECTION, R.id.cb_config_mode_prod)
//        rgModeGroup.check(modeCheckedId)
//
//        val yearCheckedId = sharedPrefs.getInt(Constants.PREF_KEY_YEAR_SELECTION, R.id.cb_config_year_release)
//        rgYearGroup.check(yearCheckedId)
    }

    private fun savePreference(key: String, value: Int) {
        val sharedPrefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(key, value)
            apply()
        }
    }
}