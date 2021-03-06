package tarehart.rlbot.routing.waypoint

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.routing.AccelerationRoutePart
import tarehart.rlbot.routing.Route
import tarehart.rlbot.routing.SteerPlan
import tarehart.rlbot.time.Duration
import tarehart.rlbot.time.GameTime
import tarehart.rlbot.tuning.ManeuverMath

class AnyFacingPreKickWaypoint(position: Vector2, expectedTime: GameTime, expectedSpeed: Double? = null, waitUntil: GameTime? = null) :
        PreKickWaypoint(position, expectedTime, expectedSpeed, waitUntil) {

    override fun isPlausibleFinalApproach(car: CarData): Boolean {

        if (ArenaModel.isCarOnWall(car)) return false
        val tminus = Duration.between(car.time, expectedTime).millis
        if (tminus > 200 || tminus < -50) return false
        val distance = car.position.flatten().distance(position)
        if (distance > 10) return false

        if (distance < 2) {
            // When a car approaches a waypoint, it tends not to hit it completely perfectly, and the angle error will
            // thrash as it passes by. Ignore this thrash when the car is super close to the waypoint.
            return true
        }
        val angleError = Math.abs(SteerUtil.getCorrectionAngleRad(car, position))
        val plausibleApproachAngle = angleError < Math.PI / 6 && tminus > 0
        val plausibleBlowPastAngle = angleError > Math.PI * 11.0 / 12.0
        if (!(plausibleApproachAngle || plausibleBlowPastAngle)) return false

        return true
    }

    override fun planRoute(car: CarData, distancePlot: DistancePlot): SteerPlan {
        val distance = this.position.distance(car.position.flatten())

        val blewPast = ManeuverMath.hasBlownPast(car, this.position)

        val waypoint = if (blewPast && waitUntil == null) {
            car.position.flatten() + car.orientation.noseVector.flatten()
        } else {
            this.position
        }

        if (this.waitUntil != null) {
            val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), waypoint, this.waitUntil - car.time))
            return SteerPlan(SteerUtil.getThereOnTime(car, SpaceTime(waypoint.toVector3(), this.waitUntil)), route)
        }
        val duration = distancePlot.getMotionAfterDistance(distance)!!.time
        val route = Route().withPart(AccelerationRoutePart(car.position.flatten(), waypoint, duration))

        if (distance > 50 && car.boost < 50) {
            return SteerPlan(SteerUtil.steerTowardGroundPosition(car, waypoint), route)
        }

        return SteerPlan(SteerUtil.steerTowardGroundPosition(car, waypoint, detourForBoost = false), route)

    }
}
