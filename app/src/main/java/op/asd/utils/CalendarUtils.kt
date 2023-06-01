package op.asd.utils

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
object CalendarUtils {

    //判断一个时间在另一个时间之后
    fun dateAfterDate2(startTime: String?, endTime: String?): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd")
        try {
            val startDate: Date = format.parse(startTime)
            val endDate: Date = format.parse(endTime)
            val start: Long = startDate.getTime()
            val end: Long = endDate.getTime()
            if (end > start) {
                return true
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return false
        }
        return false
    }



    fun dateAfterDate(startTime: String?, endTime: String?): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val startDate = format.parse(startTime)
            val endDate = format.parse(endTime)

            val calendarStart = Calendar.getInstance()
            calendarStart.time = startDate

            val calendarEnd = Calendar.getInstance()
            calendarEnd.time = endDate

            calendarEnd.after(calendarStart)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    /**
     * @return 当前日期
     */
    @SuppressLint("SimpleDateFormat")
    fun formatDateNow(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = Date()
        return simpleDateFormat.format(date)
    }
}