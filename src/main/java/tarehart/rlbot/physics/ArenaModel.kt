package tarehart.rlbot.physics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Streams
import tarehart.rlbot.AgentInput
import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.BallSlice
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.VectorUtil
import tarehart.rlbot.math.vector.Vector2
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.cpp.BallPredictorHelper
import tarehart.rlbot.planning.Goal
import tarehart.rlbot.time.Duration
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import kotlin.streams.asStream

class ArenaModel {

    private var previousBallPath: BallPath? = null
    fun simulateBall(start: BallSlice): BallPath {
        val prevPath = previousBallPath
        val ballPath: BallPath
        if (prevPath != null) {
            val prevPrediction = prevPath.getMotionAt(start.time)
            if ((prevPath.endpoint.time - start.time) > SIMULATION_DURATION &&
                    prevPrediction != null &&
                    prevPrediction.space.distance(start.space) < .3 &&
                    prevPrediction.space.flatten().distance(start.space.flatten()) < .1 &&
                    prevPrediction.velocity.distance(start.velocity) < .3 &&
                    prevPrediction.velocity.flatten().distance(start.velocity.flatten()) < .1) {

                ballPath = prevPath // Previous prediction is still legit, build on top of it.
            } else {
                ballPath = BallPredictorHelper.predictPath()
            }
        } else {
            ballPath = BallPredictorHelper.predictPath()
        }
        previousBallPath = ballPath
        return ballPath
    }

    companion object {

        val SIDE_WALL = 81.92f
        val BACK_WALL = 102.4f
        val CEILING = 40.88f
        val GRAVITY = 13f
        val BALL_RADIUS = 1.8555f

        val CORNER_BEVEL = 11.8 // 45 degree angle walls come in this far from where the rectangular corner would be.
        val CORNER_ANGLE_CENTER = Vector2(SIDE_WALL.toDouble(), BACK_WALL.toDouble()).minus(Vector2(CORNER_BEVEL, CORNER_BEVEL))


        private val majorUnbrokenPlanes = ArrayList<Plane>()
        private val backWallPlanes = ArrayList<Plane>()

        init {
            // Floor
            majorUnbrokenPlanes.add(Plane(Vector3(0.0, 0.0, 1.0), Vector3(0.0, 0.0, 0.0)))

            // Side walls
            majorUnbrokenPlanes.add(Plane(Vector3(1.0, 0.0, 0.0), Vector3((-SIDE_WALL).toDouble(), 0.0, 0.0)))
            majorUnbrokenPlanes.add(Plane(Vector3(-1.0, 0.0, 0.0), Vector3(SIDE_WALL.toDouble(), 0.0, 0.0)))

            // Ceiling
            majorUnbrokenPlanes.add(Plane(Vector3(0.0, 0.0, -1.0), Vector3(0.0, 0.0, CEILING.toDouble())))

            // 45 angle corners
            majorUnbrokenPlanes.add(Plane(Vector3(1.0, 1.0, 0.0), Vector3((-CORNER_ANGLE_CENTER.x).toFloat().toDouble(), (-CORNER_ANGLE_CENTER.y).toFloat().toDouble(), 0.0)))
            majorUnbrokenPlanes.add(Plane(Vector3(-1.0, 1.0, 0.0), Vector3(CORNER_ANGLE_CENTER.x.toFloat().toDouble(), (-CORNER_ANGLE_CENTER.y).toFloat().toDouble(), 0.0)))
            majorUnbrokenPlanes.add(Plane(Vector3(1.0, -1.0, 0.0), Vector3((-CORNER_ANGLE_CENTER.x).toFloat().toDouble(), CORNER_ANGLE_CENTER.y.toFloat().toDouble(), 0.0)))
            majorUnbrokenPlanes.add(Plane(Vector3(-1.0, -1.0, 0.0), Vector3(CORNER_ANGLE_CENTER.x.toFloat().toDouble(), CORNER_ANGLE_CENTER.y.toFloat().toDouble(), 0.0)))


            // Do the back wall major surfaces separately to avoid duplicate planes.
            backWallPlanes.add(Plane(Vector3(0.0, 1.0, 0.0), Vector3(0.0, (-BACK_WALL).toDouble(), 0.0)))
            backWallPlanes.add(Plane(Vector3(0.0, -1.0, 0.0), Vector3(0.0, BACK_WALL.toDouble(), 0.0)))
        }

        val SIMULATION_DURATION = Duration.ofSeconds(5.0)

        private val mainModel = ArenaModel()

        private val pathCache = CacheBuilder.newBuilder()
                .maximumSize(10)
                .build(object : CacheLoader<BallSlice, BallPath>() {
                    @Throws(Exception::class)
                    override fun load(key: BallSlice): BallPath {
                        synchronized(lock) {
                            // Always use a new ArenaModel. There's a nasty bug
                            // where bounces stop working properly and I can't track it down.
                            return mainModel.simulateBall(key)
                        }
                    }
                })

        private val lock = Any()

        fun isInBounds(location: Vector2): Boolean {
            return isInBounds(location.toVector3(), 0.0)
        }

        fun isInBoundsBall(location: Vector3): Boolean {
            return isInBounds(location, BALL_RADIUS.toDouble())
        }

        private fun isInBounds(location: Vector3, buffer: Double): Boolean {
            return getDistanceFromWall(location) > buffer
        }

        fun isBehindGoalLine(position: Vector3): Boolean {
            return Math.abs(position.y) > BACK_WALL
        }

        fun predictBallPath(input: AgentInput): BallPath {
            try {
                val key = BallSlice(input.ballPosition, input.time, input.ballVelocity, input.ballSpin)
                return pathCache.get(key)
            } catch (e: ExecutionException) {
                throw RuntimeException("Failed to compute ball slices!", e)
            }

        }

        fun isCarNearWall(car: CarData): Boolean {
            return getDistanceFromWall(car.position) < 2
        }

        fun getDistanceFromWall(position: Vector3): Double {
            val sideWall = SIDE_WALL - Math.abs(position.x)
            val backWall = BACK_WALL - Math.abs(position.y)
            val diagonal = CORNER_ANGLE_CENTER.x + CORNER_ANGLE_CENTER.y - Math.abs(position.x) - Math.abs(position.y)
            return Math.min(Math.min(sideWall, backWall), diagonal)
        }

        fun isCarOnWall(car: CarData): Boolean {
            return car.hasWheelContact && isCarNearWall(car) && Math.abs(car.orientation.roofVector.z) < 0.05
        }

        fun isNearFloorEdge(position: Vector3): Boolean {
            return Math.abs(position.x) > Goal.EXTENT && getDistanceFromWall(position) + position.z < 6
        }

        fun getNearestPlane(position: Vector3): Plane {

            return Streams.concat(majorUnbrokenPlanes.stream(), backWallPlanes.stream()).min { p1, p2 ->
                val p1Distance = p1.distance(position)
                val p2Distance = p2.distance(position)
                if (p1Distance > p2Distance) 1 else -1
            }.get()
        }

        fun getBouncePlane(origin: Vector3, direction: Vector3): Plane {
            val longDirection = direction.scaledToMagnitude(500.0)

            val intersectionDistances = (majorUnbrokenPlanes.asSequence() + backWallPlanes.asSequence()).asStream()
                    .collect(Collectors.toMap<Plane, Plane, Double>({p -> p}, { p ->
                        VectorUtil.getPlaneIntersection(p, origin, longDirection)?.distance(origin) ?: Double.MAX_VALUE
                    }))

            return intersectionDistances.entries.stream().min(Comparator.comparingDouble{ ent -> ent.value}).get().key
        }
    }
}