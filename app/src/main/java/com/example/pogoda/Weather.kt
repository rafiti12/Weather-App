package com.example.pogoda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Weather : Fragment(R.layout.fragment_weather) {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_weather, container, false)

        // Setting image from bundle
        CoroutineScope(Dispatchers.IO).launch{
            val image = view.findViewById<ImageView>(R.id.imageView)
            val icon = arguments?.getString("icon")
//            val str = "https://openweathermap.org/img/wn/$icon.png"
//            val url = URL(str)
//            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            activity?.runOnUiThread {
                //image.setImageBitmap(bitmap)
                val id = resources.getIdentifier("icon$icon", "drawable", "com.example.pogoda")
                image.setImageResource(id)
            }
        }

        //Setting text in textViews
        var r = arguments?.getString("name")
        r += " " + arguments?.getString("country")
        view.findViewById<TextView>(R.id.weatherName).text = r
        r = "lon: " + arguments?.getString("lon")
        r += "  lat: " + arguments?.getString("lat")
        view.findViewById<TextView>(R.id.weatherCoord).text = r
        view.findViewById<TextView>(R.id.weatherTime).text = arguments?.getString("updatedAtText")
        view.findViewById<TextView>(R.id.weatherTemp).text = arguments?.getString("temp")
        view.findViewById<TextView>(R.id.weatherPressure).text = arguments?.getString("pressure")
        view.findViewById<TextView>(R.id.weatherDesc).text = arguments?.getString("weatherDescription")

        return view
    }
}