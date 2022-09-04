package com.example.pogoda

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val SUCCESS = 0
    private val OFFLINE = -1
    private val API_PROBLEM = -2
    private val API = "42933d94c3e23104cf3a09557198b383"
    private var city = "lodz"
    private var units = "metric" // metric, default, imperial
    private var bundleWeather : Bundle = Bundle()
    private var bundleDetails : Bundle = Bundle()
    private var bundleForecast : Bundle = Bundle()
    private var currFrag = "weather"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(savedInstanceState != null){
            city = savedInstanceState.getString("city").toString()
            units = savedInstanceState.getString("units").toString()
            currFrag = savedInstanceState.getString("currFrag").toString()
            bundleWeather = savedInstanceState.getBundle("bunWeather")!!
            bundleDetails = savedInstanceState.getBundle("bunDetails")!!
            bundleForecast = savedInstanceState.getBundle("bunForecast")!!
        }
        else {
            CoroutineScope(IO).launch {
                loadFromFile("recent.json", "recent2.json")
                val r = updateData(false)
                if(r == API_PROBLEM){
                    loadFromFile("recent.json", "recent2.json")
                    runOnUiThread{
                        showAlert("Couldn't connect with API. Loaded recent data.")
                    }
                }
                else if(r == OFFLINE){
                    loadFromFile("recent.json", "recent2.json")
                    runOnUiThread{
                        showAlert("Couldnt connect with internet. Loaded recent data.")
                    }
                }
                val f1: Fragment
                if (currFrag == "menu") {
                    f1 = Menu()
                } else if (currFrag == "details") {
                    f1 = Details()
                    f1.arguments = bundleDetails
                } else if (currFrag == "forecast") {
                    f1 = Forecast()
                    f1.arguments = bundleForecast
                } else {
                    f1 = Weather()
                    f1.arguments = bundleWeather
                }
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flFragment, f1)
                    commit()
                }
            }
        }
    }

    override fun onSaveInstanceState(icicle: Bundle) {
        super.onSaveInstanceState(icicle)
        icicle.putString("currFrag", currFrag)
        icicle.putString("city", city)
        icicle.putString("units", units)
        icicle.putBundle("bunWeather", bundleWeather)
        icicle.putBundle("bunDetails", bundleDetails)
        icicle.putBundle("bunForecast", bundleForecast)
    }

    fun menuClick(view: View){
        this.currFrag = "menu"
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, Menu())
            addToBackStack(null)
            commit()
        }
    }

    fun weatherClick(view: View){
        this.currFrag = "weather"
        val f1 = Weather()
        f1.arguments = bundleWeather
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, f1)
            addToBackStack(null)
            commit()
        }
    }

    fun detailsClick(view: View){
        this.currFrag = "details"
        val f1 = Details()
        f1.arguments = bundleDetails
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, f1)
            addToBackStack(null)
            commit()
        }
    }

    fun forecastClick(view: View){
        this.currFrag = "forecast"
        val f1 = Forecast()
        f1.arguments = bundleForecast
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, f1)
            addToBackStack(null)
            commit()
        }
    }

    fun clearFavClick(view: View){
        val f = File(filesDir, "favorites.txt")
        if(!f.exists()){
            runOnUiThread{
                showAlert("List Cleared.")
            }
            return
        }
        val fw = FileWriter(f)
        fw.write("")
        fw.close()
        runOnUiThread{
            showAlert("List Cleared.")
        }
    }

    fun updateClick(view: View){
        CoroutineScope(IO).launch {
            //update favorites
            val f = File(filesDir, "favorites.txt")
            val length = f.length().toInt()
            val bytes = ByteArray(length)
            val tempor: FileInputStream
            try{
                tempor = FileInputStream(f)
            }catch(e: Exception){
                //try to update current
                city = findViewById<EditText>(R.id.menuCity).text.toString()
                units = findViewById<Spinner>(R.id.menuUnits).selectedItem.toString()
                if(units == "SI") units = "default"
                val r = updateData(false)
                if(r == SUCCESS){
                    currFrag = "weather"
                    val f1 = Weather()
                    f1.arguments = bundleWeather
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flFragment, f1)
                        addToBackStack(null)
                        commit()
                    }
                    runOnUiThread{
                        showAlert("Data Updated.")
                    }
                    return@launch
                }
                else if(r == OFFLINE){
                    runOnUiThread{
                        showAlert("Cant connect with internet.")
                    }
                    return@launch
                }
                runOnUiThread{
                    showAlert("Cant load any data.")
                }
                return@launch
            }
            tempor.use { tempor ->
                tempor.read(bytes)
            }
            val contents = String(bytes)
            val lines = contents.split("\n").dropLastWhile { it == "" }
            var r = SUCCESS
            for(i in lines){
                val cityUnit = i.split(",")
                city = cityUnit[0]
                units = cityUnit[1]
                r = updateData(true)
                if(r == API_PROBLEM){
                    city = findViewById<EditText>(R.id.menuCity).text.toString()
                    units = findViewById<Spinner>(R.id.menuUnits).selectedItem.toString()
                    if(units == "SI") units = "default"
                    if(isFavorite()){
                        loadFromFile("$city.json", city + "2.json")
                        runOnUiThread{
                            showAlert("Couldn't connect with API. Loaded recent $city data.")
                        }
                        return@launch
                    }
                    else{
                        loadFromFile("recent.json", "recent2.json")
                        runOnUiThread{
                            showAlert("Couldn't connect with API. Loaded recent data.")
                        }
                        return@launch
                    }
                }
                else if(r == OFFLINE){
                    city = findViewById<EditText>(R.id.menuCity).text.toString()
                    units = findViewById<Spinner>(R.id.menuUnits).selectedItem.toString()
                    if(units == "SI") units = "default"
                    if(isFavorite()){
                        loadFromFile("$city.json", city + "2.json")
                        runOnUiThread{
                            showAlert("Couldn't connect with internet. Loaded recent $city data.")
                        }
                        return@launch
                    }
                    else{
                        loadFromFile("recent.json", "recent2.json")
                        runOnUiThread{
                            showAlert("Couldn't connect with internet. Loaded recent data.")
                        }
                        return@launch
                    }
                }
            }
            //update current
            city = findViewById<EditText>(R.id.menuCity).text.toString()
            units = findViewById<Spinner>(R.id.menuUnits).selectedItem.toString()
            if(units == "SI") units = "default"
            r = updateData(false)
            if(r == SUCCESS){
                runOnUiThread{
                    showAlert("Data Updated.")
                }
            }
            else if(r == API_PROBLEM){
                runOnUiThread{
                    showAlert("Couldn't connect with API. Check if your spelling is correct.")
                }
            }
            else if(r == OFFLINE){
                runOnUiThread{
                    showAlert("Couldn't connect with internet.")
                }
            }
            currFrag = "weather"
            val f1 = Weather()
            f1.arguments = bundleWeather
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flFragment, f1)
                addToBackStack(null)
                commit()
            }
        }
    }

    private fun showAlert(message: String){
        val alert = AlertDialog.Builder(this)
        alert.setCancelable(true)
        alert.setTitle("Data update")
        alert.setMessage(message)
        alert.setPositiveButton("Ok") { dialog, _ ->
            dialog.cancel()
        }
        alert.show()
    }

    private fun isFavorite(): Boolean {
        val f = File(filesDir, "favorites.txt")
        if(!f.exists()) return false
        val length = f.length().toInt()
        val bytes = ByteArray(length)
        val tempor = FileInputStream(f)
        tempor.use { tempor ->
            tempor.read(bytes)
        }
        val contents = String(bytes)
        val lines = contents.split("\n")
        for(i in lines){
            val uppi = i.uppercase()
            val uppCu = city.uppercase() + "," + units.uppercase()
            if(uppi == uppCu) return true
        }
        return false
    }

    fun writeToFav(view: View){
        this.city = findViewById<EditText>(R.id.menuCity).text.toString()
        this.units = findViewById<Spinner>(R.id.menuUnits).selectedItem.toString()
        if(this.units == "SI") this.units = "default"
        CoroutineScope(IO).launch {
            val r = updateData(true)
            if(r == SUCCESS){
                if(!isFavorite()){
                    val f = File(filesDir, "favorites.txt")
                    val fw = FileWriter(f, true)
                    val res = "$city,$units\n"
                    fw.write(res)
                    fw.close()
                    runOnUiThread {
                        showAlert("Added to favorites.")
                    }
                }
                else {
                    runOnUiThread {
                        showAlert("City is already in favorites.")
                    }
                }
            }
            else if(r == API_PROBLEM){
                runOnUiThread {
                    showAlert("Couldn't connect with API. Check if your spelling is correct.")
                }
            }
            else if(r == OFFLINE){
                runOnUiThread {
                    showAlert("Couldnt connect with internet.")
                }
            }
        }
    }

    private fun writeToFile(filename: String, filename2: String, jsonObj: JSONObject, jsonObj2: JSONObject){
        try {
            val f = File(filesDir, filename)
            val fw = FileWriter(f)
            jsonObj.put("units", this.units)
            fw.write(jsonObj.toString())
            fw.close()

            val f2 = File(filesDir, filename2)
            val fw2 = FileWriter(f2)
            fw2.write(jsonObj2.toString())
            fw2.close()
        } catch (e: java.lang.Exception) {
            runOnUiThread{
                showAlert("Couldnt save recent file.")
            }
        }
    }

    private fun loadFromFile(filename: String, filename2: String){
        val f = File(filesDir, filename)
        //if(!f.exists()) return
        val length = f.length().toInt()
        val bytes = ByteArray(length)
        val tempor: FileInputStream
        try{
            tempor = FileInputStream(f)
        }catch(e: Exception){
            runOnUiThread{
                showAlert("Cant load any data.")
            }
            return
        }
        tempor.use { tempor ->
            tempor.read(bytes)
        }
        val contents = String(bytes)
        val jsonObj = JSONObject(contents)

        val main = jsonObj.getJSONObject("main")
        val sys = jsonObj.getJSONObject("sys")
        val wind = jsonObj.getJSONObject("wind")
        val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
        val coord = jsonObj.getJSONObject("coord")

        val name = jsonObj.getString("name")
        val country = sys.getString("country")
        val lon = coord.getString("lon")
        val lat = coord.getString("lat")
        val updatedAt:Long = jsonObj.getLong("dt")
        val updatedAtText = "Updated at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt*1000))
        var temp = main.getString("temp")
        val pressure = "Pressure: " + main.getString("pressure") + " hPa"
        val weatherDescription = weather.getString("description")
        val icon = weather.getString("icon")

        val humidity = "Humidity: " + main.getString("humidity") + "%"
        var tempMin = "Min Temp: " + main.getString("temp_min")
        var tempMax = "Max Temp: " + main.getString("temp_max")
        val sunriseNum:Long = sys.getLong("sunrise")
        val sunrise = "Sunrise at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(sunriseNum*1000))
        val sunsetNum:Long = sys.getLong("sunset")
        val sunset = "Sunset at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(sunsetNum*1000))
        var windSpeed = "Wind speed: " + wind.getString("speed")

        this.city = name
        this.units = jsonObj.getString("units")

        //Changing values that depend on selected unit system
        if(this.units == "metric"){
            temp += "°C"
            tempMin += "°C"
            tempMax += "°C"
            windSpeed += " meter/sec"
        }
        else if(this.units == "imperial"){
            temp += "°F"
            tempMin += "°F"
            tempMax += "°F"
            windSpeed += " miles/hour"
        }
        else{
            temp += "°K"
            tempMin += "°K"
            tempMax += "°K"
            windSpeed += " meter/sec"
        }

        bundleWeather.putString("name", name)
        bundleWeather.putString("country", country)
        bundleWeather.putString("lon", lon)
        bundleWeather.putString("lat", lat)
        bundleWeather.putString("updatedAtText", updatedAtText)
        bundleWeather.putString("temp", temp)
        bundleWeather.putString("pressure", pressure)
        bundleWeather.putString("weatherDescription", weatherDescription)
        bundleWeather.putString("icon", icon)

        bundleDetails.putString("humidity", humidity)
        bundleDetails.putString("tempMin", tempMin)
        bundleDetails.putString("tempMax", tempMax)
        bundleDetails.putString("sunrise", sunrise)
        bundleDetails.putString("sunset", sunset)
        bundleDetails.putString("windSpeed", windSpeed)

        val f2 = File(filesDir, filename2)
        val length2 = f2.length().toInt()
        val bytes2 = ByteArray(length2)
        val tempor2 = FileInputStream(f2)
        tempor2.use { tempor2 ->
            tempor2.read(bytes2)
        }
        val contents2 = String(bytes2)
        val jsonObj2 = JSONObject(contents2)
        val forecast = jsonObj2.getJSONArray("list")
        val day1 = "Tomorrow:\n" + forecast.getJSONObject(8).getJSONArray("weather").getJSONObject(0).getString("description")
        val day2 = "In 2 days:\n" + forecast.getJSONObject(16).getJSONArray("weather").getJSONObject(0).getString("description")
        val day3 = "In 3 days:\n" + forecast.getJSONObject(24).getJSONArray("weather").getJSONObject(0).getString("description")
        val day4 = "In 4 days:\n" + forecast.getJSONObject(32).getJSONArray("weather").getJSONObject(0).getString("description")

        bundleForecast.putString("day1", day1)
        bundleForecast.putString("day2", day2)
        bundleForecast.putString("day3", day3)
        bundleForecast.putString("day4", day4)
    }

    private fun updateData(addFav: Boolean): Int {
        if(isOnline(this)) {
            var response:String?
            response = try{
                URL("https://api.openweathermap.org/data/2.5/weather?q=$city&units=$units&appid=$API").readText(Charsets.UTF_8)
            }catch (e: Exception){
                println(e)
                null
            }
            if(response != null){
                val jsonObj = JSONObject(response)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val coord = jsonObj.getJSONObject("coord")

                val name = jsonObj.getString("name")
                val country = sys.getString("country")
                val lon = coord.getString("lon")
                val lat = coord.getString("lat")
                val updatedAt:Long = jsonObj.getLong("dt")
                val updatedAtText = "Updated at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt*1000))
                var temp = main.getString("temp")
                val pressure = "Pressure: " + main.getString("pressure") + " hPa"
                val weatherDescription = weather.getString("description")
                val icon = weather.getString("icon")

                val humidity = "Humidity: " + main.getString("humidity") + "%"
                var tempMin = "Min Temp: " + main.getString("temp_min")
                var tempMax = "Max Temp: " + main.getString("temp_max")
                val sunriseNum:Long = sys.getLong("sunrise")
                val sunrise = "Sunrise at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(sunriseNum*1000))
                val sunsetNum:Long = sys.getLong("sunset")
                val sunset = "Sunset at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(sunsetNum*1000))
                var windSpeed = "Wind speed: " + wind.getString("speed")

                //Changing values that depend on selected unit system
                if(this.units == "metric"){
                    temp += "°C"
                    tempMin += "°C"
                    tempMax += "°C"
                    windSpeed += " meter/sec"
                }
                else if(this.units == "imperial"){
                    temp += "°F"
                    tempMin += "°F"
                    tempMax += "°F"
                    windSpeed += " miles/hour"
                }
                else{
                    temp += "°K"
                    tempMin += "°K"
                    tempMax += "°K"
                    windSpeed += " meter/sec"
                }

                bundleWeather.putString("name", name)
                bundleWeather.putString("country", country)
                bundleWeather.putString("lon", lon)
                bundleWeather.putString("lat", lat)
                bundleWeather.putString("updatedAtText", updatedAtText)
                bundleWeather.putString("temp", temp)
                bundleWeather.putString("pressure", pressure)
                bundleWeather.putString("weatherDescription", weatherDescription)
                bundleWeather.putString("icon", icon)

                bundleDetails.putString("humidity", humidity)
                bundleDetails.putString("tempMin", tempMin)
                bundleDetails.putString("tempMax", tempMax)
                bundleDetails.putString("sunrise", sunrise)
                bundleDetails.putString("sunset", sunset)
                bundleDetails.putString("windSpeed", windSpeed)

                response = try{
                    URL("https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&units=$units&appid=$API").readText(Charsets.UTF_8)
                }catch (e: Exception){
                    println(e)
                    null
                }
                if(response != null){
                    val jsonObj2 = JSONObject(response)
                    val forecast = jsonObj2.getJSONArray("list")
                    val day1 = "Tomorrow:\n" + forecast.getJSONObject(8).getJSONArray("weather").getJSONObject(0).getString("description")
                    val day2 = "In 2 days:\n" + forecast.getJSONObject(16).getJSONArray("weather").getJSONObject(0).getString("description")
                    val day3 = "In 3 days:\n" + forecast.getJSONObject(24).getJSONArray("weather").getJSONObject(0).getString("description")
                    val day4 = "In 4 days:\n" + forecast.getJSONObject(32).getJSONArray("weather").getJSONObject(0).getString("description")

                    bundleForecast.putString("day1", day1)
                    bundleForecast.putString("day2", day2)
                    bundleForecast.putString("day3", day3)
                    bundleForecast.putString("day4", day4)

                    writeToFile("recent.json", "recent2.json", jsonObj, jsonObj2)
                    if(addFav){
                        val s1 = "$city.json"
                        val s2 = city + "2.json"
                        writeToFile(s1, s2, jsonObj, jsonObj2)
                    }

                    return SUCCESS
                }
                else return API_PROBLEM
            }
            else return API_PROBLEM
        }
        else return OFFLINE
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}