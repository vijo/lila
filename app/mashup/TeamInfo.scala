package lila.app
package mashup

import lila.user.{ User, UserRepo }
import lila.game.{ GameRepo, Game }
import lila.forum.MiniForumPost
import lila.team.{ Team, Request, RequestRepo, MemberRepo, RequestWithUser, TeamApi }
import lila.team.tube._
import lila.db.api._

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    bestPlayers: List[User],
    toints: Int,
    forumNbPosts: Int,
    forumPosts: List[MiniForumPost]) {

  def hasRequests = requests.nonEmpty
}

object TeamInfo {

  def apply(
    api: TeamApi,
    getForumNbPosts: String => Fu[Int],
    getForumPosts: String => Fu[List[MiniForumPost]])(team: Team, me: Option[User]): Fu[TeamInfo] = for {
    requests ← (team.enabled && me.??(m => team.isCreator(m.id))) ?? api.requestsWithUsers(team)
    mine = me.??(m => api.belongsTo(team.id, m.id))
    requestedByMe ← !mine ?? me.??(m => RequestRepo.exists(team.id, m.id))
    userIds ← MemberRepo userIdsByTeam team.id
    bestPlayers ← UserRepo.byIdsSortRating(userIds, 10)
    toints ← UserRepo.idsSumToints(userIds)
    forumNbPosts ← getForumNbPosts(team.id)
    forumPosts ← getForumPosts(team.id)
  } yield TeamInfo(
    mine = mine,
    createdByMe = ~me.map(m => team.isCreator(m.id)),
    requestedByMe = requestedByMe,
    requests = requests,
    bestPlayers = bestPlayers,
    toints = toints,
    forumNbPosts = forumNbPosts,
    forumPosts = forumPosts)
}
