package aaarne.tum.rmp.pathplanning

import java.awt.Color

import aaarne.tum.rmp.geometry.LineSegment
import breeze.linalg._
import breeze.plot._

private trait RandomTreePathPlanner extends PathPlanner {

  val stepSize = 0.2

  case class RRTTree(tree: Tree[Int], coordinates: Map[Int, Point]) {

    def addSample: RRTTree = {
      val point = samplePoint
      val closest = coordinates.minBy(_._2 dist point)
      val p1 = DenseVector(closest._2.x, closest._2.y)
      val p2: Vector[Double] = point
      val newCoordinate = p1 + stepSize * (p2 - p1)
      if (obstacles exists (_.lineCollides(LineSegment(newCoordinate, p1)))) this.addSample
      else {
        val i = coordinates.keys.max + 1
        RRTTree(tree.add(closest._1, i), coordinates + (i -> Point(newCoordinate(0), newCoordinate(1))))
      }
    }
  }

  object RRTTree {
    /**
      * Generate infinite stream of RRT Trees rooted at start
      *
      * @return
      */
    def from(start: Point): Stream[RRTTree] =
      Stream.iterate(RRTTree(Leaf(0), Map(0 -> start)))(_.addSample)
  }

  var pastTrees: List[RRTTree] = Nil

  def pointPairEyeContact(p1: Point, p2: Point): Boolean =
    obstacles forall (o => !o.lineCollides(LineSegment(p1, p2)))

  def isPointVisible(point: Point)(rrt: RRTTree): Boolean = rrt match {
    case RRTTree(_, coordinates) => coordinates.values exists (pointPairEyeContact(point, _))
  }
}

/**
  * RRT implementation starting a single RRT at the start node
  */
trait SimpleRRT extends RandomTreePathPlanner {

  override def plan(start: Point, destination: Point): Option[Path] = {

    val finalTree = RRTTree.from(start) find isPointVisible(destination)

    pastTrees = finalTree match {
      case None => Nil
      case Some(t) => t :: Nil
    }

    finalTree map {
      case RRTTree(t, c) =>
        val finalNode = c.filter {
          case (i, p) => obstacles forall (o => !o.lineCollides(LineSegment(p, destination)))
        }.head._1

        (t pathTo finalNode map c) :+ destination
    }
  }
}

/**
  * RRT implementation starting a RRT on both, the start and the goal node
  */
trait RRT extends RandomTreePathPlanner {

  def lastAddedPoint(t: RRTTree): Point = t.coordinates(t.coordinates.keys.max)

  def connectable(trees: (RRTTree, RRTTree)): Boolean = trees match {
    case (t1, t2) =>
      (List(lastAddedPoint(t1), lastAddedPoint(t2)) zip List(t2, t1)) exists {
        case (p, tree) => isPointVisible(p)(tree)
      }
  }

  def computePath(startTree: RRTTree, goalTree: RRTTree): Path = (startTree, goalTree) match {
    case (RRTTree(t1, points1), RRTTree(t2, points2)) =>
      val paths = for {
        (finalNode1, p1) <- points1.toStream
        (finalNode2, p2) <- points2.toStream
        if pointPairEyeContact(p1, p2)
      } yield {
        val path1 = t1 pathTo finalNode1 map points1
        val path2 = t2 pathTo finalNode2 map points2
        path1 ++ path2.reverse
      }
      paths.head
  }

  override def plan(start: Point, destination: Point): Option[Path] =
    (RRTTree.from(start) zip RRTTree.from(destination)) find connectable map {
      case (startTree, goalTree) =>
        pastTrees = startTree :: goalTree :: Nil
        computePath(startTree, goalTree)
    }
}

trait RRTViz extends PathPlannerDemo with RandomTreePathPlanner {

  override def plotSingleQuery(f: Plot, color: Color, verbose: Boolean): Unit = {

    pastTrees.zipWithIndex foreach {
      case (tree, index) => println(s"Tree ${index + 1} has ${tree.coordinates.keys.max + 1} nodes.")
    }

    val points = pastTrees flatMap {
      case RRTTree(tree, coordinates) => coordinates.values
    }

    if (verbose)
      f += scatter(
        x = DenseVector(points map (_.x): _*),
        y = DenseVector(points map (_.y): _*),
        size = _ => 0.3,
        colors = _ => new Color(color.getRed, color.getGreen, color.getBlue, 60)
      )
  }
}

object SimpleRRTDemo extends RRTViz with SimpleRRT {
  override val title = "RRT Path Planning (single tree)"
}

object RRTDemo extends RRTViz with RRT {
  override val title = "RRT Path Planning (symmetric)"
}
