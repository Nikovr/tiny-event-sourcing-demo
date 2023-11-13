package ru.quipy.controller

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.quipy.MongoTemplateEventStore.Companion.logger
import ru.quipy.api.*
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.*
import ru.quipy.projections.*
import java.util.*
import kotlin.collections.ArrayList

@RestController
@RequestMapping("/tasks")
class TaskController (
    val taskEsService: EventSourcingService<UUID, TaskAggregate, TaskAggregateState>,
    private val projectCacheRepository: ProjectCacheRepository,
    private val taskCacheRepository: TaskCacheRepository,
    private val userCacheRepository: UserCacheRepository,
) {
    @PostMapping("/{taskTitle}/{projectId}")
    fun createTask(@PathVariable taskTitle: String,@PathVariable projectId: UUID,  @RequestParam creatorId: String) : TaskCreatedEvent{
        return taskEsService.create { it.create(UUID.randomUUID(), taskTitle, creatorId, projectId)  }
    }

    @GetMapping("/get/{taskId}")
    fun getTask(@PathVariable taskId: UUID) : TaskInfo {
        val getTask = taskCacheRepository.findById(taskId)
        val executors = getTask.get().executors
        val executorEntityList = ArrayList<UserInfo>()
        for ( user in executors ) {
            val curExecutor = userCacheRepository.findById(user).get()
            val userInfo = UserInfo(curExecutor.userId, curExecutor.userName, curExecutor.userNickName)
            executorEntityList.add(userInfo)
        }
        return TaskInfo(getTask.get().taskId, getTask.get().projectId, getTask.get().statusId, getTask.get().creatorId, getTask.get().title, executorEntityList)
    }

    @PostMapping("/change/{taskId}")
    fun changeTaskTitle(@PathVariable taskId: UUID, @RequestParam taskTitle: String) : TaskTitleChangedEvent {
        return taskEsService.update(taskId) {it.changeTitle(taskId ,taskTitle)}
    }

    @PostMapping("executors/{taskId}/{userId}")
    fun addExecutor(@PathVariable taskId: UUID, @PathVariable userId: UUID) : TaskAddedExecutorEvent {
        return taskEsService.update(taskId) {it.addExecutor(userId)}
    }

    @PostMapping("statuses/{taskId}/{statusId}")
    fun assignStatus(@PathVariable taskId: UUID, @PathVariable statusId: UUID) : StatusAssignedToTaskEvent {
        return taskEsService.update(taskId) {it.assignStatus(taskId, statusId)}
    }

    @DeleteMapping("delete/{taskId}/{userId}")
    fun deleteExecutor(@PathVariable taskId: UUID, @PathVariable userId: UUID) : TaskRemovedExecutor {
        return taskEsService.update(taskId) {it.removeExecutor(userId)}
    }

}