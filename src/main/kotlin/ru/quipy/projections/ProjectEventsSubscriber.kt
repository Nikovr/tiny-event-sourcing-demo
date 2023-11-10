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
import javax.annotation.PostConstruct

@Service
class ProjectEventsSubscriber {

    val logger: Logger = LoggerFactory.getLogger(ProjectEventsSubscriber::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(TaskAggregate::class, "some-meaningful-name") {

            `when`(TaskCreatedEvent::class) { event ->
                logger.info("Task created: {}", event.title)
                logger.info("Task created 1: {}", event.title)
            }
//            `when`(StatusCreatedEvent::class) { event ->
//                logger.info("Tag created: {}", event.projectId)
//            }
//
//            `when`(TagAssignedToTaskEvent::class) { event ->
//                logger.info("Tag {} assigned to task {}: ", event.tagId, event.taskId)
//            }
        }
    }
}

//@Document("transactions-projects-cache")
//data class Project(
//        @Id
//
//)
//
//@Repository
//interface ProjectCacheRepository: MongoRepository<