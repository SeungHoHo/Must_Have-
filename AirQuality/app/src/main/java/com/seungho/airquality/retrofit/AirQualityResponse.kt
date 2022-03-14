package com.seungho.airquality.retrofit

data class AirQualityResponse (
    val status : String,     //상태 "success"
    val data : Data
) {
    data class Data (
        val country : String, //국가명 "대한민국"
        val state : String, //주명 "충청남도"
        val city : String, //도시명 "성연면"
        val location : Location,
        val current: Current
    ) {
        data class Current(
            val pollution : Pollution,
            val weather : Weather
        ) {
            data class Pollution (
                val aqius : Int, // 미국 AQI, AQI = Air Quality Index의 약자
                val mainus : String, // 미국 AQI의 주요 오염물질
                val aqicn : Int,
                val maincn : String,
                val ts: String // 타임스탬프(시간)
            )
            data class Weather (
                val tp : Int, // 섭씨온도
                val ic : String, // 날씨 아이콘 코드
            )
        }
        data class Location (
            val coordinates : List<Double>, //좌표
            val type: String //형식
       )
    }
}