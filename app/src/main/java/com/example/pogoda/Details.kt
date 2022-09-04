package com.example.pogoda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment


class Details : Fragment(R.layout.fragment_details) {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_details, container, false)

        //Setting text in textViews
        view.findViewById<TextView>(R.id.detailsHum).text = arguments?.getString("humidity")
        view.findViewById<TextView>(R.id.detailsTempMin).text = arguments?.getString("tempMin")
        view.findViewById<TextView>(R.id.detailsTempMax).text = arguments?.getString("tempMax")
        view.findViewById<TextView>(R.id.detailsSunrise).text = arguments?.getString("sunrise")
        view.findViewById<TextView>(R.id.detailsSunset).text = arguments?.getString("sunset")
        view.findViewById<TextView>(R.id.detailsWind).text = arguments?.getString("windSpeed")


        return view
    }
}