package ru.quipy.controller

import netscape.javascript.JSObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.quipy.MongoTemplateEventStore.Companion.logger
import ru.quipy.api.ProjectAggregate
import ru.quipy.api.ProjectCreatedEvent
import ru.quipy.api.StatusCreatedEvent
import ru.quipy.api.UserInvitedEvent
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.*
import ru.quipy.projections.*
import java.util.*
import kotlin.collections.ArrayList

@RestController
@RequestMapping("/projects")
class ProjectController(
        val projectEsService: EventSourcingService<UUID, ProjectAggregate, ProjectAggregateState>,
        private val projectCacheRepository: ProjectCacheRepository,
        private val userCacheRepository: UserCacheRepository,
) {

    @PostMapping("/{projectTitle}")
    fun createProject(@PathVariable projectTitle: String, @RequestParam creatorId: String) : ProjectCreatedEvent {
        return projectEsService.create { it.create(UUID.randomUUID(), projectTitle, creatorId) }
    }

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID) : ProjectInfo{
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


    @PostMapping("/status/{projectId}/{statusTitle}")
    fun createStatus(@PathVariable projectId: UUID, @PathVariable statusTitle: String) : StatusCreatedEvent {
        return projectEsService.update(projectId) { it.createStatus(statusTitle) }
    }

    @PostMapping("/invite/{projectId}/{userId}")
    fun inviteUser(@PathVariable projectId: UUID, @PathVariable userId: UUID) : UserInvitedEvent? {
        return projectEsService.update(projectId) {it.inviteUser(userId)}
    }

}