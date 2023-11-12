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
class ProjectEventsSubscriber (
    private val projectCacheRepository: ProjectCacheRepository,
) {

    val logger: Logger = LoggerFactory.getLogger(ProjectEventsSubscriber::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(ProjectAggregate::class, "transactions::projects-cache") {

            `when`(ProjectCreatedEvent::class) { event ->
                projectCacheRepository.save(Project(event.projectId, event.title, event.creatorId, event.createdAt, ArrayList<UUID>(), ArrayList<UUID>()))
                logger.info("Project created: {}", event.title)
            }
            `when`(StatusCreatedEvent::class) { event ->
                val projectOptional = projectCacheRepository.findById(event.projectId)
                val project = projectOptional.get()
                val statuses = project.statuses
                if(!statuses.contains(event.statusId)) {
                    project.statuses.add(event.statusId)
                    projectCacheRepository.save(project)
                } else {
                    logger.info("Status already exists: {}", project.projectId)
                }
            }
            `when`(UserInvitedEvent::class) { event ->
                val projectOptional = projectCacheRepository.findById(event.projectId)
                val project = projectOptional.get()
                val members = project.members
                if(!members.contains(event.userId)) {
                    project.statuses.add(event.userId)
                    projectCacheRepository.save(project)
                } else {
                    logger.info("Executor already exists: {}", project.projectId)
                }
            }
        }
    }
}

@Document("transactions-projects-cache")
data class Project(
        @Id
        val projectId: UUID,
        val title: String,
        val creatorId: String,
        val createdAt: Long,
        var members: ArrayList<UUID>,
        var statuses: ArrayList<UUID>,
)

@Repository
interface ProjectCacheRepository: MongoRepository<Project, UUID>

//TODO: create repo for statuses?