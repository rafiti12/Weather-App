package com.example.pogoda

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class Forecast : Fragment(R.layout.fragment_forecast) {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_forecast, container, false)

        //Setting text in textViews
        view.findViewById<TextView>(R.id.forecast1day).text = arguments?.getString("day1")
        view.findViewById<TextView>(R.id.forecast2day).text = arguments?.getString("day2")
        view.findViewById<TextView>(R.id.forecast3day).text = arguments?.getString("day3")
        view.findViewById<TextView>(R.id.forecast4day).text = arguments?.getString("day4")


        return view
    }
}