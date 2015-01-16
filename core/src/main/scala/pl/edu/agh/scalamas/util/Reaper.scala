/*
 * Copyright 2013 - 2015, Daniel Krzywicki <daniel.krzywicki@agh.edu.pl>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.scalamas.util

import akka.actor._
import scala.concurrent.{ExecutionContext, Promise}
import akka.actor.Terminated
import scala.util.Success
import scala.concurrent.duration.FiniteDuration
import scala.collection.mutable

object Reaper {
  def actorsTerminate(actors: Seq[ActorRef])(implicit system: ActorSystem) = {
    val p = Promise[Unit]()
    val callback = () => p.complete(Success(()))
    system.actorOf(Props(classOf[Reaper], actors, callback))
    p.future
  }

  def terminateAfter(actor: ActorRef, duration: FiniteDuration)(implicit system: ActorSystem, executionContext: ExecutionContext) = {
    system.scheduler.scheduleOnce(duration, actor, PoisonPill)
    actorsTerminate(List(actor))
  }
}

class Reaper(souls: Seq[ActorRef], callback: () => Unit) extends Actor with ActorLogging {
  val activeSouls = mutable.Set[ActorRef](souls: _*)
  activeSouls foreach (context watch _)
  checkSouls

  def receive = {
    case Terminated(soul) if activeSouls.contains(soul) =>
      activeSouls -= soul
      checkSouls
  }

  def checkSouls = if (activeSouls.isEmpty) {
    log info "no more active souls"
    callback()
    context stop self
  }
}