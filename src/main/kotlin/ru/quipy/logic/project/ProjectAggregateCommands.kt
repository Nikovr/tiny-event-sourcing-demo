package ru.quipy.logic

import org.springframework.beans.factory.annotation.Autowired
import ru.quipy.api.ProjectCreatedEvent
import ru.quipy.api.StatusCreatedEvent
import ru.quipy.api.UserInvitedEvent
import ru.quipy.api.project.*
import ru.quipy.logic.user.userCacheRepository
import ru.quipy.projections.ProjectCacheRepository
import ru.quipy.projections.ProjectInfo
import ru.quipy.projections.UserInfo
import java.util.*

@Autowired
lateinit var projectCacheRepository: ProjectCacheRepository
// Commands : takes something -> returns event
// Here the commands are represented by extension functions, but also can be the class member functions

fun ProjectAggregateState.create(id: UUID, title: String, creatorId: String): ProjectCreatedEvent {
    return ProjectCreatedEvent(
        projectId = id,
        title = title,
        creatorId = creatorId,
    )
}

fun ProjectAggregateState.createStatus(name: String): StatusCreatedEvent {
    if (projectStatuses.values.any { it.name == name }) {
        throw IllegalArgumentException("Status already exists: $name")
    }
    return StatusCreatedEvent(projectId = this.getId(), statusId = UUID.randomUUID(), statusName = name)
}

fun ProjectAggregateState.inviteUser(id: UUID): UserInvitedEvent {
    val check = userCacheRepository.findById(id).get()
    if (projectUsers.values.any { it.userId == id }) {
        throw IllegalArgumentException("User already exists: $id")
    }
    return UserInvitedEvent(projectId = this.getId(), userId = id)
}

fun getProjectbyId(projectId: UUID) : ProjectInfo {
    val getProject = projectCacheRepository.findById(projectId).get()
    val userList = getProject.members
    val userEntityList = ArrayList<UserInfo>()
    for ( user in userList ) {
        val curMember = userCacheRepository.findById(user).get()
        val userInfo = UserInfo(curMember.userId, curMember.userName, curMember.userNickName)
        userEntityList.add(userInfo)
    }
    return ProjectInfo(getProject.projectId, getProject.title, getProject.creatorId, getProject.createdAt, userEntityList, getProject.statuses)
}