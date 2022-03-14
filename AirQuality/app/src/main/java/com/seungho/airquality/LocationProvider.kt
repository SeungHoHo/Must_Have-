package com.seungho.airquality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.lang.Exception

class LocationProvider(val context: Context) {

    private var location: Location? = null
    private var locationManager: LocationManager? = null

    init {
        getLocation()
    }

    private fun getLocation(): Location? {
        try {
            //위치 시스템 서비스 가져오기
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var gpsLocation: Location? = null
            var networkLocation: Location? = null

            //GPS/Network Provider 활성화 상태 확인
            val isGpsEnabled: Boolean =
                locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled: Boolean =
                locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                // GPS 또는 Network Provider 둘 다 사용 불가능한 상황이면 null을 반환
                return null
            } else {
                val hasFineLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION // ACCESS_COARSE_LOCATION 보다 더 정밀한 위치 정보 얻음
                )
                val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION // 도시 Block 단위의 정밀도의 위치 정보를 얻을 수 있음
                )
                //만약 두 개의 권한이 없다면 null을 반환함
                if (hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED ||
                        hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) return null

                //네트워크를 통한 위치 파악이 가능한 경우에 위치를 가져옴
                if (isNetworkEnabled) {
                    networkLocation =
                        locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                //GPS를 통한 위치 파악이 가능한 경우에 위치를 가져옴
                if (isGpsEnabled) {
                    gpsLocation =
                        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }

                if (gpsLocation != null && networkLocation != null) {
                    //만약 두 개의 위치가 있다면 정확도 높은 것으로 선택합니다.
                    if (gpsLocation.accuracy > networkLocation.accuracy) {
                        location = gpsLocation
                        return gpsLocation
                    } else {
                        location = networkLocation
                        return networkLocation
                    }
                } else {
                    //만약 가능한 위치 정보가 한 개만 있는 경우
                    if (gpsLocation != null ) {
                        location = gpsLocation
                    }

                    if (networkLocation != null) {
                        location = networkLocation
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location
    }

    // 위도 정보를 가져오는 함수
    fun getLocationLatitude(): Double {
        return location?.latitude ?: 0.0
    }

    // 경도 정보를 가져오는 함수
    fun getLocationLongitude(): Double {
        return location?.longitude ?: 0.0
    }
}