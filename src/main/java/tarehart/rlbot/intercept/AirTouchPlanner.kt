package tarehart.rlbot.intercept

import tarehart.rlbot.input.CarData
import tarehart.rlbot.math.SpaceTime
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.planning.SteerUtil
import tarehart.rlbot.steps.strikes.InterceptStep
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

object AirTouchPlanner {

    private const val AERIAL_RISE_RATE = 5.0
    private const val SUPER_JUMP_RISE_RATE = 11.0
    const val BOOST_NEEDED_FOR_AERIAL = 20.0
    const val NEEDS_AERIAL_THRESHOLD = ManeuverMath.MASH_JUMP_HEIGHT
    private const val MAX_JUMP_HIT = NEEDS_AERIAL_THRESHOLD
    const val NEEDS_JUMP_HIT_THRESHOLD = 3.2
    private const val CAR_BASE_HEIGHT = ManeuverMath.BASE_CAR_Z
    private const val MAX_FLIP_HIT = NEEDS_JUMP_HIT_THRESHOLD


    fun checkAerialReadiness(car: CarData, intercept: Intercept): AerialChecklist {

        val checklist = AerialChecklist()
        checkLaunchReadiness(checklist, car, intercept)
        val secondsTillIntercept = Duration.between(car.time, intercept.time).seconds
        val tMinus = getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept)
        checklist.timeForIgnition = tMinus < 0.1
        checklist.notSkidding = !ManeuverMath.isSkidding(car)
        checklist.hasBoost = car.boost >= BOOST_NEEDED_FOR_AERIAL

        return checklist
    }

    fun checkJumpHitReadiness(car: CarData, intercept: Intercept): LaunchChecklist {

        val checklist = LaunchChecklist()
        checkLaunchReadiness(checklist, car, intercept)
        val strikeProfile = intercept.strikeProfile
        val jumpHitTime = strikeProfile.strikeDuration.seconds
        checklist.timeForIgnition = Duration.between(car.time, intercept.time).seconds < jumpHitTime
        return checklist
    }

    fun checkSideHitReadiness(car: CarData, intercept: Intercept): LaunchChecklist {

        val checklist = LaunchChecklist()
        checkLaunchReadiness(checklist, car, intercept)
        checklist.linedUp = true // TODO: calculate this properly
        val strikeProfile = intercept.strikeProfile
        val jumpHitTime = strikeProfile.strikeDuration.seconds
        checklist.timeForIgnition = Duration.between(car.time, intercept.time).seconds < jumpHitTime
        return checklist
    }

    fun checkDiagonalHitReadiness(car: CarData, intercept: Intercept): LaunchChecklist {

        val checklist = LaunchChecklist()
        checkLaunchReadiness(checklist, car, intercept)
        checklist.linedUp = true // TODO: calculate this properly
        val strikeProfile = intercept.strikeProfile
        val jumpHitTime = strikeProfile.strikeDuration.seconds
        checklist.timeForIgnition = Duration.between(car.time, intercept.time).seconds < jumpHitTime
        return checklist
    }

    fun checkFlipHitReadiness(car: CarData, intercept: Intercept): LaunchChecklist {
        val checklist = LaunchChecklist()
        checkLaunchReadiness(checklist, car, intercept)
        checklist.timeForIgnition = Duration.between(car.time, intercept.time).seconds <= intercept.strikeProfile.strikeDuration.seconds
        return checklist
    }

    private fun checkLaunchReadiness(checklist: LaunchChecklist, car: CarData, intercept: Intercept) {

        val correctionAngleRad = SteerUtil.getCorrectionAngleRad(car, intercept.space)
        val secondsTillIntercept = Duration.between(car.time, intercept.time).seconds

        checklist.linedUp = Math.abs(correctionAngleRad) < Math.PI / 30
        checklist.closeEnough = secondsTillIntercept < 4

        checklist.upright = car.orientation.roofVector.dotProduct(Vector3(0.0, 0.0, 1.0)) > .99
        checklist.onTheGround = car.hasWheelContact
    }

    fun isVerticallyAccessible(carData: CarData, intercept: SpaceTime): Boolean {
        val secondsTillIntercept = Duration.between(carData.time, intercept.time).seconds

        if (intercept.space.z < NEEDS_AERIAL_THRESHOLD) {
            val tMinus = secondsTillIntercept - getJumpHitStrikeProfile(intercept.space.z).strikeDuration.seconds
            return tMinus >= -0.1
        }

        if (carData.boost > BOOST_NEEDED_FOR_AERIAL) {
            val tMinus = getAerialLaunchCountdown(intercept.space.z, secondsTillIntercept)
            return tMinus >= -0.1
        }
        return false
    }

    fun isJumpHitAccessible(carData: CarData, intercept: SpaceTime): Boolean {
        if (intercept.space.z > MAX_JUMP_HIT) {
            return false
        }

        val secondsTillIntercept = Duration.between(carData.time, intercept.time).seconds
        val (travelDelay) = getJumpHitStrikeProfile(intercept.space.z)
        val tMinus = secondsTillIntercept - travelDelay
        return tMinus >= -0.1
    }

    fun getJumpHitStrikeProfile(height: Double): StrikeProfile {
        // If we have time to tilt back, the nose will be higher and we can cheat a little.
        val requiredHeight = height * .7
        val jumpTime = ManeuverMath.secondsForMashJumpHeight(requiredHeight).orElse(.8)
        return StrikeProfile(jumpTime, 10.0, .15, StrikeProfile.Style.JUMP_HIT)
    }

    fun getDiagonalJumpHitStrikeProfile(height: Double): StrikeProfile {
        // If we have time to tilt back, the nose will be higher and we can cheat a little.
        val requiredHeight = height * .7
        val jumpTime = ManeuverMath.secondsForMashJumpHeight(requiredHeight).orElse(.8)
        return StrikeProfile(jumpTime, 10.0, .15, StrikeProfile.Style.DIAGONAL_HIT)
    }

    fun getSideHitStrikeProfile(height: Double): StrikeProfile {
        val jumpTime = ManeuverMath.secondsForMashJumpHeight(height).orElse(.8)
        return StrikeProfile(jumpTime, 10.0, .15, StrikeProfile.Style.SIDE_HIT)
    }

    fun getStraightOnStrikeProfile(height: Double): StrikeProfile {
        if (isFlipHitAccessible(height)) {
            return InterceptStep.FLIP_HIT_STRIKE_PROFILE
        }
        if (height < MAX_JUMP_HIT) {
            return getJumpHitStrikeProfile(height)
        }
        if (height > 10) {
            return InterceptStep.AERIAL_STRIKE_PROFILE
        }
        return StrikeProfile(0.0, 10.0, .25, StrikeProfile.Style.AERIAL)
    }

    fun getStrikeProfile(height: Double, approachAngle: Double): StrikeProfile {

        if (approachAngle < Math.PI / 8) {
            return getStraightOnStrikeProfile(height)
        }

        if (height < MAX_JUMP_HIT) {
            if (approachAngle < Math.PI * 3 / 8) {
                return getDiagonalJumpHitStrikeProfile(height)
            }
            return getSideHitStrikeProfile(height)
        }
        return InterceptStep.AERIAL_STRIKE_PROFILE
    }

    fun isFlipHitAccessible(height: Double): Boolean {
        return height <= MAX_FLIP_HIT
    }

    private fun getAerialLaunchCountdown(height: Double, secondsTillIntercept: Double): Double {
        val expectedAerialSeconds = (height - CAR_BASE_HEIGHT) / AERIAL_RISE_RATE
        return secondsTillIntercept - expectedAerialSeconds
    }

    fun expectedSecondsForSuperJump(height: Double): Double {
        return (height - CAR_BASE_HEIGHT) / SUPER_JUMP_RISE_RATE
    }

    fun getBoostBudget(carData: CarData): Double {
        return carData.boost - BOOST_NEEDED_FOR_AERIAL - 5.0
    }
}
