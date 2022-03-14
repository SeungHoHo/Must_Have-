package com.seungho.airquality

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.seungho.airquality.databinding.ActivityMainBinding
import com.seungho.airquality.retrofit.AirQualityResponse
import com.seungho.airquality.retrofit.AirQualityService
import com.seungho.airquality.retrofit.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    // 런타임 권한 요청시 필요한 요청 코드
    private val PERMISSION_REQUEST_CODE = 100

    // 요청할 권한 리스트
    var REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // 위치 서비스 요청시 필요한 런처
    lateinit var getGPSPermissionLauncher: ActivityResultLauncher<Intent>

    // 위도/경도를 가져올때 필요함
    lateinit var locationProvider: LocationProvider

    // 위도/경도 저장
    var latitude: Double = 0.0
    var longitude : Double = 0.0

    val startMapActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), object : ActivityResultCallback<ActivityResult> {
            override fun onActivityResult(result: ActivityResult?) {
                if (result?.resultCode ?: Activity.RESULT_CANCELED == Activity.RESULT_OK) {
                    latitude = result?.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                    longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                    updateUI()
                }
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()
        setRefreshButton()
        setFab()

    }

    private fun setFab() {
        binding.btnGoogleMap.setOnClickListener {
            val intent = Intent(this, GoogleMapActivity::class.java)
            intent.putExtra("settingLat" , latitude)
            intent.putExtra("settingLng" , longitude)
            startMapActivityResult.launch(intent)
        }
    }

    //새로고침 기능 구현
    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            showProgress()
            updateUI()
        }
    }

    //로딩기능 구현
    fun showProgress() {
        binding.progressBar.visibility = View.VISIBLE
    }

    //로딩기능 숨기기
    fun hideProgress() {
        binding.progressBar.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        locationProvider = LocationProvider(this)

        //위도와 경도 정보를 가져옴
        if (latitude == 0.0 || longitude == 0.0) {
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }

        if (latitude != 0.0 || longitude != 0.0) {
          //현재 위치를 가져오고 UI 업데이트
              //현재 위치 가져오기
            val address = getCurrentAddress(latitude, longitude) // 주소가 null이 아니면 UI 업데이트
            address?.let {
                binding.cityTextView.text = it.thoroughfare
                binding.countryTextView.text = "${it.countryName} ${it.adminArea}"
            }

            //현재 미세먼지 농도 가져오고 UI 업데이트
            getAQIData(latitude, longitude)
        } else {
            Toast.makeText(this, "위도와 경도 정보를 가져올 수 없습니다. 새로고침을 눌러주세요", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAQIData(latitude: Double, longitude: Double) {
        val retrofitAPI = RetrofitService.getInstance().create(AirQualityService::class.java)

        retrofitAPI.getAQIData(latitude.toString(), longitude.toString(), "603e526d-e087-4922-be7d-f13284456bb4")
            .enqueue(object : Callback<AirQualityResponse>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<AirQualityResponse>,
                    response: Response<AirQualityResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { updateAirUI(it) }
                        binding.stateTextView.text = "데이터 불러오기 성공"
                        hideProgress()
                    } else {
                        Toast.makeText(this@MainActivity, "업데이트 실패함", Toast.LENGTH_SHORT).show()
                        binding.stateTextView.text = "데이터 불러오기 실패"
                    }
                }

                override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                    t.printStackTrace()
                }

            })
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateAirUI(airQualityData : AirQualityResponse) {
        val pollutionData = airQualityData.data.current.pollution
        val weatherData = airQualityData.data.current.weather

        binding.pm1CountTextView.text = pollutionData.aqius.toString()
        binding.weatherStateTextView.text = "${weatherData.tp} ℃"

        val dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime()
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        binding.timeTextView.text = dateTime.format(dateFormatter).toString()

        when(pollutionData.aqius) {
            in 0..50 -> {
                binding.pm1StateTextView.text = "좋음"
                binding.pm1ImageView.setImageResource(R.drawable.bg_good)
            }

            in 51..100 -> {
                binding.pm1StateTextView.text = "보통"
                binding.pm1ImageView.setImageResource(R.drawable.bg_soso)
            }

            in 101..150 -> {
                binding.pm1StateTextView.text = "나쁨"
                binding.pm1ImageView.setImageResource(R.drawable.bg_bad)
            }

            else -> {
                binding.pm1StateTextView.text = "매우 나쁨"
                binding.pm1ImageView.setImageResource(R.drawable.bg_worst)
            }
        }

        when(weatherData.ic) {
            "01d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w01d)
                binding.weatherNameTextView.text = "맑음(낮)"
                binding.weatherImageView.setImageResource(R.drawable.bg_good)
            }
            "01n" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w01n)
                binding.weatherNameTextView.text = "맑음(밤)"
                binding.weatherImageView.setImageResource(R.drawable.bg_good)
            }
            "02d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w02d)
                binding.weatherNameTextView.text = "구름(낮)"
                binding.weatherImageView.setImageResource(R.drawable.bg_soso)
            }
            "02n" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w02n)
                binding.weatherNameTextView.text = "구름(밤)"
                binding.weatherImageView.setImageResource(R.drawable.bg_soso)
            }
            "03d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w03d)
                binding.weatherImageView.setImageResource(R.drawable.bg_soso)
                binding.weatherNameTextView.text = "흩어진 구름"
            }
            "04d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w04d)
                binding.weatherImageView.setImageResource(R.drawable.bg_soso)
                binding.weatherNameTextView.text = "부서진 구름"
            }
            "09d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w09d)
                binding.weatherImageView.setImageResource(R.drawable.bg_bad)
                binding.weatherNameTextView.text = "소나기"
            }
            "10d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w10d)
                binding.weatherImageView.setImageResource(R.drawable.bg_worst)
                binding.weatherNameTextView.text = "비(낮)"
            }
            "10n" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w10n)
                binding.weatherImageView.setImageResource(R.drawable.bg_worst)
                binding.weatherNameTextView.text = "비(밤)"
            }
            "11d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w11d)
                binding.weatherImageView.setImageResource(R.drawable.bg_worst)
                binding.weatherNameTextView.text = "뇌우"
            }
            "13d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w13d)
                binding.weatherImageView.setImageResource(R.drawable.bg_worst)
                binding.weatherNameTextView.text = "눈"
            }
            "50d" -> {
                binding.weatherStateImageView.setImageResource(R.drawable.w50d)
                binding.weatherImageView.setImageResource(R.drawable.bg_bad)
                binding.weatherNameTextView.text = "안개"
            }

        }
    }

    // 위도와 경도를 기준으로 실제 주소를 가져옴
    private fun getCurrentAddress(latitude: Double, longitude: Double): Address? {
        val geocoder = Geocoder(this, Locale.getDefault()) // Address 객체는 주소와 관련된 여러 정보를 가지고 있음
        val addresses: List<Address>?

        addresses = try { //Geocoder 객체를 이용하여 위도와 경도로부터 리스트를 가져옴
            geocoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException: IOException) {
            Toast.makeText(this, "지오코더 서비스 사용불가합니다.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 위도, 경도 입니다", Toast.LENGTH_LONG).show()
            return null
        }

        //에러는 아니지만 주소가 발견되지 않은 경우
        if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null
        }

        val address: Address = addresses[0]

        return address
    }

    private fun checkAllPermissions() {
        if (!isLocationServicesAvailable()) { //1. 위치 서비스(GPS)가 켜져있는지 확인합니다.
            showDialogForLocationServiceSetting()
        } else {  //2. 런타임 앱 권한이 모두 허용되어있는지 확인합니다.
            isRunTimePermissionsGranted()
        }
    }

    private fun isLocationServicesAvailable(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    private fun isRunTimePermissionsGranted() { // 위치 퍼미션을 가지고 있는지 체크합니다.
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) { // 권한이 한 개라도 없다면 퍼미션 요청을 합니다.
            ActivityCompat.requestPermissions(
                this@MainActivity, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            var checkResult = true

            /// 모든 퍼미션을 허용했는지 체크함
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break
                }
            }
            if (checkResult) {
            } else {
                Toast.makeText(this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요", Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }
    }

    private fun showDialogForLocationServiceSetting() {

        //먼저 ActivityResultLauncher를 설정해줍니다. 이 런처를 이용하여 결과 값을 리턴해야하는 인텐트를 실행할 수 있습니다.
        getGPSPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> //결과 값을 받았을 때 로직을 작성해줍니다.
            if (result.resultCode == Activity.RESULT_OK) { //사용자가 GPS 를 활성화 시켰는지 확인합니다.
                if (isLocationServicesAvailable()) {
                    isRunTimePermissionsGranted()
                } else { //위치 서비스가 허용되지 않았다면 앱을 종료합니다.
                    Toast.makeText(
                        this@MainActivity, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        })
        builder.setNegativeButton("취소", DialogInterface.OnClickListener { dialog, id ->
            dialog.cancel()
            Toast.makeText(
                this@MainActivity, "기기에서 위치서비스(GPS) 설정 후 사용해주세요.", Toast.LENGTH_SHORT
            ).show()
            finish()
        })
        builder.create().show()
    }
}