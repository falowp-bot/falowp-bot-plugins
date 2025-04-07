package com.blr19c.falowp.bot.plugins.idle


import com.blr19c.falowp.bot.system.api.SendMessage
import com.blr19c.falowp.bot.system.plugin.Plugin
import com.blr19c.falowp.bot.system.plugin.Plugin.Task.cronScheduling
import com.blr19c.falowp.bot.system.systemConfigProperty
import com.blr19c.falowp.bot.system.web.webclient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.Summary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjusters


@Plugin(
    name = "摸鱼",
    tag = "聊天",
    desc = "每天10点推送"
)
class Idle {


    private val idle = cronScheduling("0 0 10 * * ?") {
        val inputStream =
            webclient().get("https://www.shuyz.com/githubfiles/china-holiday-calender/master/holidayCal.ics")
                .bodyAsChannel().toInputStream()
        val calendar = CalendarBuilder().build(inputStream)
        val now = LocalDate.now()
        val holidayLines = mutableListOf<String>()
        for (closestHoliday in getClosestHolidays(calendar, now)) {
            val startDate = closestHoliday.getStartDateTime()!!
            val endDate = closestHoliday.getEndDateTime()!!
            val summary = closestHoliday.summary.value
            val distanceSummary = ChronoUnit.DAYS.between(now, startDate)
            val sumSummaryDay = ChronoUnit.DAYS.between(startDate, endDate) + 1
            val line = "距离【$summary】还有: ${distanceSummary}天-共${sumSummaryDay}天"
            holidayLines.add(line)
        }
        val todayDate = DateTimeFormatter.ofPattern("yyyy年MM月dd日").format(now)
        val todayWeek = DateTimeFormatter.ofPattern("EEEE").format(now)
        val todayLine = """今天是${todayDate}，${todayWeek}。"""

        val title = """【${systemConfigProperty("nickname")}摸鱼办】提醒您:"""
        val myLine = "摸鱼人！即使今天是开工第${now.dayOfYear}天，也一定不要忘记摸鱼哦！"
        val body1 =
            """$todayLine $myLine 有事没事起身去茶水间，去厕所，去廊道走走，别总在工位上坐着，钱是老板的，但健康是自己的。"""

        val lastBetween = ChronoUnit.DAYS.between(now, now.with(TemporalAdjusters.lastDayOfMonth()))
        val payWagesLast = """离【月底发工资】：${lastBetween}天"""
        val payWages05 = """离【05号发工资】：${betweenPayday(5)}"""
        val payWages10 = """离【10号发工资】：${betweenPayday(10)}"""
        val payWages15 = """离【15号发工资】：${betweenPayday(15)}"""
        val payWages20 = """离【20号发工资】：${betweenPayday(20)}"""
        val payWages25 = """离【25号发工资】：${betweenPayday(25)}"""
        val week = """距离【双休】还有：${betweenWeek()}"""
        val lineList =
            listOf(
                title,
                body1,
                payWagesLast,
                payWages05,
                payWages10,
                payWages15,
                payWages20,
                payWages25,
                week,
                holidayLines.joinToString("\n")
            )
        val line = lineList.joinToString("\n")
        this.sendAllGroup(SendMessage.builder(line).build())
    }


    /**
     * 获取最近的发薪
     */
    private fun betweenPayday(dayOfMonth: Int): String {
        val now = LocalDate.now()
        var payday: LocalDate = LocalDate.of(now.year, now.month, dayOfMonth)
        if (now.dayOfMonth > dayOfMonth) {
            payday = payday.plusMonths(1)
        }
        val between = ChronoUnit.DAYS.between(now, payday)
        return if (between == 0.toLong()) "就在今天!!" else between.toString().plus("天")
    }

    /**
     * 获取最近的双休
     */
    private fun betweenWeek(): String {
        val now = LocalDate.now()
        val current = now.dayOfWeek
        if (DayOfWeek.SATURDAY == current || DayOfWeek.SUNDAY == current) {
            return "就在今天!!"
        }
        val between = ChronoUnit.DAYS.between(now, now.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)))
        return between.toString().plus("天")
    }


    /**
     * 获取最近的假期
     */
    private fun getClosestHolidays(calendar: Calendar, currentDate: LocalDate): List<VEvent> {
        val allHolidays = calendar.getComponents<VEvent>(Component.VEVENT)
        val map = mutableMapOf<String, VEvent>()
        var lastSummaryComment: String? = null
        var lastHoliday: VEvent? = null
        for (holiday in allHolidays
            .filter { it.getEndDateTime() != null }
            .filter { it.getStartDateTime() != null }
            .filter { it.getStartDateTime()!!.isAfter(currentDate) }
            .sortedBy { it.getEndDateTime() }) {
            val summarySplit = holiday.summary.value.split(" ")
            if (summarySplit.size < 2) continue
            if (summarySplit[1] == "补班") continue
            val summaryComment = summarySplit[0] + summarySplit[1]
            if (lastSummaryComment == null) {
                lastSummaryComment = summaryComment
                var propertyList: ContentCollection<Property> = holiday!!.propertyList
                propertyList = propertyList.remove(holiday.getProperty<Summary>(Property.SUMMARY).get())
                propertyList = propertyList.add(Summary(lastSummaryComment))
                lastHoliday = VEvent(propertyList as PropertyList)
                map[summaryComment] = lastHoliday
                continue
            }
            if (lastSummaryComment == summaryComment) {
                var propertyList: ContentCollection<Property> = lastHoliday!!.propertyList
                propertyList = propertyList.remove(lastHoliday.getProperty<DtEnd<Temporal>>(Property.DTEND).get())
                propertyList = propertyList.add(holiday.getProperty<DtEnd<Temporal>>(Property.DTEND).get())
                lastHoliday = VEvent(propertyList as PropertyList)
                map[summaryComment] = lastHoliday
                continue
            }
            if (map.size == 2) {
                break
            }
            lastSummaryComment = null
        }
        return map.values.toList()
    }


    private fun DateTimePropertyAccessor.getStartDateTime(): LocalDate? {
        val startDate = this.getDateTimeStart<Temporal>()?.date ?: return null
        if (startDate is LocalDateTime) {
            return startDate.toLocalDate()
        }
        return LocalDate.from(startDate)
    }

    private fun DateTimePropertyAccessor.getEndDateTime(): LocalDate? {
        val endDate = this.getDateTimeEnd<Temporal>()?.date ?: return null
        if (endDate is LocalDateTime) {
            return endDate.toLocalDate()
        }
        return LocalDate.from(endDate)
    }

    init {
        idle.register()
    }
}
