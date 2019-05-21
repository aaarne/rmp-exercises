package aaarne.tum.rmp.geometry

import breeze.linalg.{Vector, norm, sum}

case class LineSegment(p1: Vector[Double], p2: Vector[Double]) {

  assert(p1.length == 2 && p2.length == 2)

  def intersects(other: LineSegment, allow_terminal_intersection: Boolean = false): Boolean = {
    val result = (ccw(p1, other.p1, other.p2) != ccw(p2, other.p1, other.p2)) && (ccw(p1, p2, other.p1) != ccw(p1, p2, other.p2))
    val intersectionAtVertex = List(other.p1, other.p2) map dist exists (d => d < 1e-6)
    result && (!allow_terminal_intersection || !intersectionAtVertex)
  }

  private def ccw(a: Vector[Double], b: Vector[Double], c: Vector[Double]): Boolean =
    (c(1) - a(1)) * (b(0) - a(0)) > (b(1) - a(1)) * (c(0) - a(0))

  def onLine(p: Vector[Double], tol: Double = 1e-6): Boolean = dist(p) match {
    case d if d >= -tol && d <= tol => true
    case _ => false
  }

  def projection(p: Vector[Double]): Vector[Double] = {
    val d = p2 - p1

    val u = sum((p - p1) *:* d) / sum(d *:* d) match {
      case x if x > 1 => 1.0
      case x if x < 0 => 0.0
      case x => x
    }
    p1 + u * d
  }

  def dist(p: Vector[Double]): Double = norm(projection(p) - p)

}