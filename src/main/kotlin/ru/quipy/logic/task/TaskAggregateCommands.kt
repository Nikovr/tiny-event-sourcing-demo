package ru.quipy.logic

import org.springframework.beans.factory.annotation.Autowired
import ru.quipy.MongoTemplateEventStore.Companion.logger
import ru.quipy.api.*
import ru.quipy.api.project.*
import ru.quipy.logic.user.userCacheRepository
import ru.quipy.projections.TaskCacheRepository
import ru.quipy.projections.TaskInfo
import ru.quipy.projections.UserInfo
import java.util.*


@Autowired
lateinit var taskCacheRepository: TaskCacheRepository

fun TaskAggregateState.create(id: UUID, title: String, creatorId: String, projectId: UUID): TaskCreatedEvent {
    return TaskCreatedEvent(
            taskId = id,
            title = title,
            creatorId = creatorId,
            projectId = projectId
            )
}

fun TaskAggregateState.changeTitle(id: UUID, title: String): TaskTitleChangedEvent {
    return TaskTitleChangedEvent(
        taskId = id,
        title = title
    )
}

fun TaskAggregateState.assignStatus(id: UUID, statusId: UUID): StatusAssignedToTaskEvent {
    return StatusAssignedToTaskEvent(
        taskId = id,
        statusId = statusId,
    )
}

fun TaskAggregateState.addExecutor(id: UUID): TaskAddedExecutorEvent {
    if (executors.values.any { it.userId == id }) {
        throw IllegalArgumentException("User already exists: $id")
    }
    return TaskAddedExecutorEvent(taskId = this.getId(), executorId = id)
}

fun TaskAggregateState.removeExecutor(id: UUID): TaskRemovedExecutor {
    if (!executors.values.any { it.userId == id }) {
        throw IllegalArgumentException("User doesn't exists: $id")
    }

    return TaskRemovedExecutor(taskId = this.getId(), executorId = id)
}

fun getTaskbyId(taskId: UUID) : TaskInfo {
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