package dashboard.lib

import xml.NodeSeq
import java.net.URL
import net.liftweb.util.Helpers._
import org.scala_tools.time.Imports._

sealed abstract class Movement { def imgTag: NodeSeq }
case class Unchanged() extends Movement { val imgTag = NodeSeq.Empty }
case class NewEntry() extends Movement { val imgTag =  <img src="new_entry_icon.png" alt="New"/> }
case class Up() extends Movement { val imgTag = <img src="up_arrow_icon.png" alt="Up"/> }
case class Down() extends Movement { val imgTag = <img src="down_arrow_icon.png" alt="Down"/> }


case class HitReport(url: String, percent: Double, hits: Int, referrers: List[String], movement: Movement = Unchanged()) {
  def summary = "%s %.1f%% (%d hits)" format(url, percent, hits)

  lazy val referrerHostCounts = referrers.flatMap(url => tryo { new URL(url).getHost })
    .groupBy(identity).mapValues(_.size).toList.sortBy(_._2).reverse

  lazy val referrerPercents: List[(String, Double)] = referrerHostCounts.map { case (host, count) =>
    host -> (count * 100.0 / hits)
  }

}



case class TheTopTen(hits: List[HitReport] = Nil, lastUpdated: DateTime = DateTime.now, firstUpdated: DateTime = DateTime.now) {
  private val fmt = "d MMM yyyy h:mm:ss a"

  def ageString = "%s to %s" format (
    firstUpdated.toString(fmt),
    lastUpdated.toString(fmt)
  )

  def diff(newList: List[HitReport], clickStream: ClickStream): TheTopTen = {
    TheTopTen(newList.zipWithIndex map { case (hit, idx) => diffedHit(hit, idx) }, clickStream.lastUpdated, clickStream.firstUpdated)
  }

  def diffedHit(newHitReport: HitReport, newIdx: Int) = {
    val currentHitReportIdx = hits.indexWhere(_.url == newHitReport.url)

    val movement =
      if (currentHitReportIdx == -1) NewEntry()
      else if (newIdx < currentHitReportIdx) Up()
      else if (newIdx > currentHitReportIdx) Down()
      else Unchanged()

    newHitReport.copy(movement = movement)
  }
}