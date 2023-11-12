package ru.quipy.projections

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import ru.quipy.api.*
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct


@Service
class TaskEventsSubscriber(
        private val taskCacheRepository: TaskCacheRepository,
) {
    val logger: Logger = LoggerFactory.getLogger(TaskEventsSubscriber::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager
    final var str = "deafult"
    var uuidStart: UUID = UUID.nameUUIDFromBytes(str.toByteArray())
    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(TaskAggregate::class, "transactions::tasks-cache") {

            `when`(TaskCreatedEvent::class) { event ->
                taskCacheRepository.save(Task(event.taskId, event.projectId, uuidStart, event.creatorId, event.title, ArrayList<UUID>()))
                logger.info("Task created: {}", event.title)
            }
            `when`(TaskAddedExecutorEvent::class) { event ->
                val taskOptional = taskCacheRepository.findById(event.taskId)
                val task = taskOptional.get()
                val executors = task.executors
                if(!executors.contains(event.executorId)) {
                    task.executors.add(event.executorId)
                    taskCacheRepository.save(task)
                } else {
                    logger.info("Executor already exist: {}", task.taskId)
                }
            }
            `when`(StatusAssignedToTaskEvent::class) { event ->
                val taskOptional = taskCacheRepository.findById(event.taskId)
                val task = taskOptional.get()
                val statusNew = event.statusId
                task.statusId = statusNew
                taskCacheRepository.save(task)

            }
            `when`(TaskTitleChangedEvent::class) { event ->
                val taskOptional = taskCacheRepository.findById(event.taskId)
                val task = taskOptional.get()
                task.title = event.title
                taskCacheRepository.save(task)
            }
            `when`(TaskRemovedExecutor::class) { event ->
                val taskOptional = taskCacheRepository.findById(event.taskId)
                val task = taskOptional.get()
                val executors = task.executors
                if(executors.contains(event.executorId)) {
                    task.executors.remove(event.executorId)
                    taskCacheRepository.save(task)
                } else {
                    logger.info("Error while deleting executor , executor doen't exist: {}", task.taskId)
                }
            }
        }
    }
}

@Document("transactions::tasks-cache")
data class Task(
    @Id
    val taskId: UUID,
    val projectId: UUID,
    var statusId: UUID,
    val creatorId: String,
    var title: String,
    var executors:  ArrayList<UUID>
        )

@Repository
interface TaskCacheRepository: MongoRepository<Task, UUID>