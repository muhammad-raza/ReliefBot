package tarehart.rlbot.integration.shots

import org.junit.Test
import rlbot.gamestate.*
import tarehart.rlbot.integration.StateSettingAbstractTest
import tarehart.rlbot.integration.StateSettingTestCase
import tarehart.rlbot.integration.asserts.BallTouchAssert
import tarehart.rlbot.integration.asserts.PlaneBreakAssert
import tarehart.rlbot.physics.ArenaModel
import tarehart.rlbot.planning.SoccerGoal
import tarehart.rlbot.steps.state.StateVector
import tarehart.rlbot.time.Duration
import tarehart.rlbot.tuning.ManeuverMath

class BouncingBallShotTest: StateSettingAbstractTest() {

    @Test
    fun testStraightShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-50F, 10F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(30F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, -20F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(2.0),
                        delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.6))))

        runTestCase(testCase)
    }

    @Test
    fun testAngledShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-50F, 30F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(30F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(30F, -20F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(2.5),
                        delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(2.3))))

        runTestCase(testCase)
    }

    @Test
    fun testVeryAngledShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-50F, 30F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(30F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(50F, 10F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(5.0)),

                        BallTouchAssert(Duration.ofSeconds(2.6))))

        runTestCase(testCase)
    }

    @Test
    fun testSideShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-60F, 70F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(30F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(30F, 70F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat(), 0F))
                        )),
                hashSetOf(PlaneBreakAssert(
                        plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                        extent = SoccerGoal.EXTENT,
                        timeLimit = Duration.ofSeconds(4.5),
                        delayWhenBallFloating = true),

                        // TODO: This is a very slow time to ball
                        BallTouchAssert(Duration.ofSeconds(4.3))))

        runTestCase(testCase)
    }


    @Test
    fun testSlowShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(-40F, 50F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(10F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 30F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(3.0),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.6))))

        runTestCase(testCase)
    }

    @Test
    fun testMediumRollingShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(40F, 50F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(-22F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 30F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(2.3),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.4))))

        runTestCase(testCase)
    }

    @Test
    fun chaseTheBallShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(30F, 70F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(-27F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(20F, 40F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, -Math.PI.toFloat(), 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(2.3),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.4))))

        runTestCase(testCase)
    }

    @Test
    fun testFastShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(40F, 50F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(-32F, 0F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 30F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(1.4),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.3))))

        runTestCase(testCase)
    }

    @Test
    fun testMovingTowardsCarShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 50F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -10F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 30F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(3.0),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.0))))

        runTestCase(testCase)
    }

    @Test
    fun testMovingSlowlyTowardsCarShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 50F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -5F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 30F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(1.3),
                                delayWhenBallFloating = true),

                        BallTouchAssert(Duration.ofSeconds(1.2))))

        runTestCase(testCase)
    }

    @Test
    fun testMovingFastTowardsCarShot() {

        val testCase = StateSettingTestCase(
                GameState()
                        .withBallState(BallState().withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 50F, ArenaModel.BALL_RADIUS))
                                .withVelocity(StateVector(0F, -20F, 12.1F))
                                .withAngularVelocity(StateVector.ZERO)
                        ))
                        .withCarState(0, CarState().withBoostAmount(50F).withPhysics(PhysicsState()
                                .withLocation(StateVector(0F, 10F, ManeuverMath.BASE_CAR_Z.toFloat()))
                                .withVelocity(StateVector(0F, 0F, 0F))
                                .withAngularVelocity(StateVector.ZERO)
                                .withRotation(DesiredRotation(0F, Math.PI.toFloat() / 2, 0F))
                        )),
                hashSetOf(
                        PlaneBreakAssert(
                                plane = PlaneBreakAssert.ENEMY_GOAL_PLANE,
                                extent = SoccerGoal.EXTENT,
                                timeLimit = Duration.ofSeconds(3.0)),

                        BallTouchAssert(Duration.ofSeconds(0.6))))

        runTestCase(testCase)
    }

}