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
import ru.quipy.api.user.UserCreatedEvent
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct


@Service
class UserEventsSubscriber(
    private val userCacheRepository: UserCacheRepository,
) {
    val logger: Logger = LoggerFactory.getLogger(UserEventsSubscriber::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager
    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(UserAggregate::class, "transactions::user-cache") {

            `when`(UserCreatedEvent::class) { event ->
                userCacheRepository.save(User(event.userId, event.userName, event.userNickName, event.userPassword))
                logger.info("User created: {}", event.userId)
            }
        }
    }
}

@Document("transactions::user-cache")
data class User(
    @Id
    val userId: UUID,
    val userName: String,
    val userNickName: String,
    val userPassword: String
)

data class UserInfo(
        val userId: UUID,
        val userName: String,
        val userNickName: String,
)

@Repository
interface UserCacheRepository: MongoRepository<User, UUID>