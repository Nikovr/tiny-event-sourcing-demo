package ru.quipy.logic.user

import org.springframework.beans.factory.annotation.Autowired
import ru.quipy.api.user.UserCreatedEvent
import ru.quipy.logic.UserAggregateState
import ru.quipy.projections.UserCacheRepository
import ru.quipy.projections.UserInfo
import java.util.*


// Commands : takes something -> returns event
// Here the commands are represented by extension functions, but also can be the class member functions
@Autowired
lateinit var userCacheRepository: UserCacheRepository

fun UserAggregateState.create(id: UUID, name: String, nickname: String, password: String): UserCreatedEvent {
    return UserCreatedEvent(
        userId = id,
        userName = name,
        userNickName = nickname,
        userPassword = password
    )
}

fun getUserbyId(userID :UUID): UserInfo {
    try {
        val curExecutor = userCacheRepository.findById(userID).get()
        return UserInfo(curExecutor.userId, curExecutor.userName, curExecutor.userNickName)
    } catch (e : Exception) {
        throw NoSuchElementException("We do not have such user : $e");
    }
}