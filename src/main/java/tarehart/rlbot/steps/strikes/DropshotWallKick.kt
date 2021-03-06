package tarehart.rlbot.steps.strikes

import tarehart.rlbot.carpredict.Impact
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.Ray
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.ArenaModel
import java.awt.Color

class DropshotWallKick : KickStrategy {

    override fun getKickDirection(car: CarData, ballPosition: Vector3): Vector3 {
        val toBall = ballPosition.minus(car.position)
        return getDirection(car, ballPosition, toBall)
    }

    override fun getKickDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {
        return getDirection(car, ballPosition, easyKick)
    }

    override fun looksViable(car: CarData, ballPosition: Vector3): Boolean {
        return true
    }

    private fun getDirection(car: CarData, ballPosition: Vector3, easyKick: Vector3): Vector3 {

        val wallPlanes = ArenaModel.getWallPlanes()

        wallPlanes.forEach { wallPlane ->
            val direction = wallPlane.normal
            val hit = getFirstPlaneBreak(Ray(car.position, direction), wallPlanes)

            hit?.let {
                if (Math.abs(it.direction.dotProduct(wallPlane.normal)) > .99) {
                    // We're hitting a plane square on.
                    car.renderer.drawLine3d(Color.ORANGE, car.position.toRlbot(), it.position.toRlbot())
                }

            }
        }

        ArenaModel.wallIntersectionPoints.forEach {
            car.renderer.drawLine3d(Color.LIGHT_GRAY, car.position.toRlbot(), it.withZ(car.position.z).toRlbot())
        }

        return easyKick
    }

    private fun getFirstPlaneBreak(ray: Ray, planes: List<Plane>): Impact? {
        var closest = Double.MAX_VALUE
        var impact: Impact? = null
        for (p in planes) {
            val spot = VectorUtil.getPlaneIntersection(p, ray.position, ray.direction * 10000.0) ?: continue
            val distance = ray.position.distanceSquared(spot)
            if (distance < closest) {
                closest = distance
                impact = Impact(spot, p.normal)
            }
        }

        return impact
    }

}
