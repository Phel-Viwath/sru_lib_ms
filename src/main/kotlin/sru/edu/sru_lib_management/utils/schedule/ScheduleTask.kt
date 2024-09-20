/*
 * Copyright (c) 2024.
 * @Author Phel Viwath
 */

package sru.edu.sru_lib_management.utils.schedule

import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import sru.edu.sru_lib_management.core.domain.service.AttendService
import sru.edu.sru_lib_management.core.domain.service.BookService
import sru.edu.sru_lib_management.utils.IndochinaDateTime.indoChinaDate
import sru.edu.sru_lib_management.utils.IndochinaDateTime.indoChinaTime
import sru.edu.sru_lib_management.utils.OpeningTime.ELEVEN_AM
import sru.edu.sru_lib_management.utils.OpeningTime.FIVE_PM
import sru.edu.sru_lib_management.utils.OpeningTime.FIVE_THIRTY_PM
import sru.edu.sru_lib_management.utils.OpeningTime.SEVEN_AM
import sru.edu.sru_lib_management.utils.OpeningTime.SEVEN_THIRTY_PM
import sru.edu.sru_lib_management.utils.OpeningTime.TWO_PM
import java.time.LocalTime

@Component
class ScheduleTask(
    private val attendService: AttendService,
    private val bookService: BookService
) {

    private val logger = LoggerFactory.getLogger(ScheduleTask::class.java)

    @Scheduled(cron = "0 25 11 * * ?", zone = "Asia/Phnom_Penh")
    @Scheduled(cron = "0 25 17 * * ?", zone = "Asia/Phnom_Penh")
    @Scheduled(cron = "0 50 19 * * ?", zone = "Asia/Phnom_Penh")
    fun autoUpdateExitingTimes() = runBlocking {
        val timeToAutoExit = listOf(
            LocalTime.parse("11:05:00"),
            LocalTime.parse("17:05:00"),
            LocalTime.parse("19:40:00")
        )
        try {
            attendService.getAllAttendByDate(indoChinaDate()).onEach { attend ->
                if (attend.exitingTimes == null) {
                    val newExitingTime = when (indoChinaTime()) {
                        in SEVEN_AM..ELEVEN_AM -> timeToAutoExit[0]
                        in TWO_PM..FIVE_PM -> timeToAutoExit[1]
                        in FIVE_THIRTY_PM..SEVEN_THIRTY_PM -> timeToAutoExit[2]
                        else -> indoChinaTime()
                    }
                    logger.info("Updating exiting time for student ${attend.studentId} to $newExitingTime")
                    if (attend.studentId != null){
                        attendService.updateExitingTime(attend.studentId.toString(), indoChinaTime())
                    }
                    if (attend.staffId != null)
                        attendService.updateExitingTime(attend.staffId, indoChinaTime())
                }
            }
        } catch (e: Exception) {
            logger.error("Error in autoUpdateMorningExitingTimes", e)
        }
    }

    /// Delete all book that inactive in 30 day
    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Phnom_Penh")
    @Transactional
    suspend fun deleteInactiveBook(){
        bookService.emptyTrash()
    }


}