package site.jiyang.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import site.jiyang.model.Issue as IssueModel

object Issues : IntIdTable() {
    val issueId = text("issueId").index()
    val json = text("jsonData")
}

class Issue(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Issue>(Issues)

    var issueId by Issues.issueId
    var json by Issues.json
}
