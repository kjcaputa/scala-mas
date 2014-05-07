package org.paramas.mas

import akka.actor.{ActorContext, Props, Actor}
import scala.util.Random
import org.paramas.emas.config.AppConfig
import org.paramas.mas.LogicTypes._

object RootEnvironment {

  /**
   * Message send from children islands to the root environment to request migration of the provided agents.
   * @param agents the agents to be migrated
   */
  case class Migrate(agents: Seq[Agent])

  /**
   * Message send from the root environment to its children islands to request the addition of an agent to the island.
   * @param agent the agent to be added to the island
   */
  case class Add(agent: Agent)

  /**
   * Props for a RootEnvironment.
   * @param islandProps the props of the island children
   * @param islandsNumber the number of children to spawn
   * @return the props for a RootEnvironment
   */
  def props(islandProps: Props, islandsNumber: Int) = Props(classOf[RootEnvironment], islandProps, islandsNumber)

  /**
   * Generic migration behaviour, which delegates the migration to the parent of the current context.
   * This parent is assumed to be a RootEnvironment
   * @param context current context
   * @return the result of a migration meeting - an empty list
   */
  def migration(implicit context: ActorContext): MeetingFunction = {
    case (Migration(cap), agents) =>
      //      agents grouped(cap) foreach { context.parent ! Migrate(_)}
      context.parent ! Migrate(agents);
      List.empty
  }
}

/**
 * Root environment for the simulation. Handles migrations between children islands.
 * @param islandProps the props of the island children
 * @param islandsNumber the number of children to spawn
 */
class RootEnvironment(islandProps: Props, islandsNumber: Int) extends Actor {
  require(islandsNumber > 0)

  import RootEnvironment._

  val islands = Array.tabulate(islandsNumber)(i => context.actorOf(islandProps, s"island-$i"))

  def receive = {
    case Migrate(agents) =>
      agents.foreach {
        agent => randomIsland ! Add(agent)
      }
  }

  def randomIsland = islands(Random.nextInt(islands.size))
}