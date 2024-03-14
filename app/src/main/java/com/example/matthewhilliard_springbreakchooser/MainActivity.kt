package com.example.matthewhilliard_springbreakchooser

import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() , SensorEventListener, TextToSpeech.OnInitListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var edittext: EditText
    private lateinit var currentLanguage: String
    private lateinit var currentLanguageCode: String
    private lateinit var tts: TextToSpeech
    private var greetingSpoken = false
    private val vacationSpots = mapOf(
        "English" to listOf("New York City", "Los Angeles"),
        "Spanish" to listOf("Barcelona", "Cancun"),
        "Chinese" to listOf("Beijing", "Shanghai"),
        "French" to listOf("Paris", "Bordeaux")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }

        edittext = findViewById(R.id.editText)
        val listview = findViewById<ListView>(R.id.listView)
        val languages = arrayOf("English", "Spanish", "Chinese", "French")
        val languageCode = arrayOf("en-US", "es-ES", "zh-CN", "fr-FR")
        currentLanguage = "English"
        currentLanguageCode = "en-US"

        val arrayadapter: ArrayAdapter<String> = ArrayAdapter(
            this, android.R.layout.simple_list_item_1, languages
        )

        tts = TextToSpeech(this, this)

        listview.adapter = arrayadapter

        listview.setOnItemClickListener { adapterView, view, i, id ->
            currentLanguage = languages[i]
            currentLanguageCode = languageCode[i]
            edittext.text = null
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something")
            result.launch(intent)
        }
    }

    private val result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data
            if(data != null){
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if(!results.isNullOrEmpty()){
                    edittext.setText(results[0])
                }
            }
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val x = sensorEvent.values[0]
        val y = sensorEvent.values[1]
        val z = sensorEvent.values[2]

        val sum = abs(x) + abs(y) + abs(z)

        if(sum > 16  && !greetingSpoken){
            val cities = vacationSpots[currentLanguage]
            if (!cities.isNullOrEmpty()) {
                val randomCity = cities.random()
                openMap(randomCity)
                greetingSpoken = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Nothing needed here
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale(currentLanguageCode)
            tts.setLanguage(locale)
        }
    }

    private fun openMap(location: String) {
        tts.speak(getGreeting(), TextToSpeech.QUEUE_FLUSH, null, null)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("geo:0,0?q=$location")
        startActivity(intent)
    }

    private fun getGreeting(): String {
        return when (currentLanguage) {
            "English" -> "Hello"
            "Spanish" -> "Hola"
            "Chinese" -> "你好"
            "French" -> "Bonjour"
            else -> "Hello"
        }
    }

    override fun onResume() {
        super.onResume()
        greetingSpoken = false
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}