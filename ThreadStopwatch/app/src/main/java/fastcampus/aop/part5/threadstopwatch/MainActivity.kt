package fastcampus.aop.part5.threadstopwatch

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import fastcampus.aop.part5.threadstopwatch.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    var isRunning = false
    var timer: Timer? = null
    var time = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener(this)
        binding.btnRefresh.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_start -> {
                if (isRunning) {
                    pause()
                } else {
                    start()
                }
            }
            R.id.btn_refresh -> {
                refresh()
            }
        }
    }



    private fun start() {
        binding.btnStart.text = "일시정지"
        binding.btnStart.setBackgroundColor(getColor(R.color.red))
        isRunning = true

        //스톱위치 시작로직
        timer = timer(period = 10) {
            time++

            val milli_second = time % 100
            val second = (time % 6000) / 100
            val minute = time / 6000

            runOnUiThread {
                if (isRunning) {
                    //milli
                    binding.tvMillisecond.text = if (milli_second < 10) ".0${milli_second}" else ".${milli_second}"
                    //second
                    binding.tvSecond.text = if (second < 10) ":0${second}" else ":${second}"
                    //minute
                    binding.tvMinute.text = "${minute}"
                }
            }
        }
    }

    private fun pause() {
        binding.btnStart.text = "시작"
        binding.btnStart.setBackgroundColor(getColor(R.color.blue))

        isRunning = false
        timer?.cancel()
    }

    private fun refresh() {
        binding.btnStart.text = "시작"
        binding.btnStart.setBackgroundColor(getColor(R.color.blue))
        isRunning = false

        time = 0
        binding.tvMillisecond.text = ".00"
        binding.tvSecond.text = ":00"
        binding.tvMinute.text = "00"
    }

}