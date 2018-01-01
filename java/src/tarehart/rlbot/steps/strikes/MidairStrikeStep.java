package tarehart.rlbot.steps.strikes;

import tarehart.rlbot.AgentInput;
import tarehart.rlbot.AgentOutput;
import tarehart.rlbot.input.CarData;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.VectorUtil;
import tarehart.rlbot.math.vector.Vector2;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.GoalUtil;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.steps.BlindStep;
import tarehart.rlbot.steps.Step;
import tarehart.rlbot.steps.TapStep;
import tarehart.rlbot.steps.rotation.PitchToPlaneStep;
import tarehart.rlbot.steps.rotation.RollToPlaneStep;
import tarehart.rlbot.steps.rotation.YawToPlaneStep;
import tarehart.rlbot.time.Duration;
import tarehart.rlbot.time.GameTime;
import tarehart.rlbot.ui.ArenaDisplay;

import java.awt.*;
import java.util.Optional;

import static java.lang.Math.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static tarehart.rlbot.physics.ArenaModel.predictBallPath;
import static tarehart.rlbot.intercept.InterceptCalculator.getAerialIntercept;
import static tarehart.rlbot.planning.SteerUtil.getCorrectionAngleRad;
import static tarehart.rlbot.planning.WaypointTelemetry.set;
import static tarehart.rlbot.tuning.BotLog.println;

public class MidairStrikeStep implements Step {
    private static final double SIDE_DODGE_THRESHOLD = Math.PI / 4;
    public static final int DODGE_TIME = 400;
    public static final double DODGE_DISTANCE = 6;
    public static final Duration MAX_TIME_FOR_AIR_DODGE = Duration.Companion.ofMillis(1500);
    public static final double UPWARD_VELOCITY_MAINTENANCE_ANGLE = .25;
    public static final double YAW_OVERCORRECT = .1;
    public static final double PITCH_OVERCORRECT = .1;
    private static final double EFFECTIVE_AIR_BOOST_ACCELERATION = 18; // Normally 19ish, but we'll be wiggling
    private int confusionCount = 0;
    private Plan plan;
    private GameTime lastMomentForDodge;
    private GameTime beginningOfStep;
    private Duration timeInAirAtStart;
    private SpaceTime intercept;

    public MidairStrikeStep(Duration timeInAirAtStart) {
        this.timeInAirAtStart = timeInAirAtStart;
    }

    public Optional<AgentOutput> getOutput(AgentInput input) {

        if (plan != null) {
            if (plan.isComplete()) {
                return empty();
            }
            return plan.getOutput(input);
        }

        if (lastMomentForDodge == null) {
            lastMomentForDodge = input.getTime().plus(MAX_TIME_FOR_AIR_DODGE).minus(timeInAirAtStart);
            beginningOfStep = input.getTime();
        }

        BallPath ballPath = predictBallPath(input);
        CarData car = input.getMyCarData();
        Vector3 offset = GoalUtil.getOwnGoal(car.getTeam()).getCenter().scaledToMagnitude(3).minus(new Vector3(0, 0, .6));
        if (intercept != null) {
            Vector3 goalToBall = intercept.getSpace().minus(GoalUtil.getEnemyGoal(car.getTeam()).getNearestEntrance(intercept.getSpace(), 4));
            offset = goalToBall.scaledToMagnitude(2.5);
            if (goalToBall.magnitude() > 110) {
                offset = new Vector3(offset.getX(), offset.getY(), -.2);
            }
        }
        Optional<SpaceTime> interceptOpportunity = getAerialIntercept(car, ballPath, offset);
        if (!interceptOpportunity.isPresent()) {
            confusionCount++;
            if (confusionCount > 3) {
                // Front flip out of confusion
                plan = new Plan().withStep(new TapStep(2, new AgentOutput().withPitch(-1).withJump()));
                return plan.getOutput(input);
            }
            return of(new AgentOutput().withBoost());
        }
        intercept = interceptOpportunity.get();
        set(intercept.getSpace().flatten(), car.getTeam());
        Vector3 carToIntercept = intercept.getSpace().minus(car.getPosition());
        long millisTillIntercept = Duration.Companion.between(input.getTime(), intercept.getTime()).getMillis();
        double distance = car.getPosition().distance(input.getBallPosition());
        println("Midair strike running... Distance: " + distance, input.getPlayerIndex());

        double correctionAngleRad = getCorrectionAngleRad(car, intercept.getSpace());

        if (input.getTime().isBefore(lastMomentForDodge) && distance < DODGE_DISTANCE) {
            // Let's flip into the ball!
            if (abs(correctionAngleRad) <= SIDE_DODGE_THRESHOLD) {
                println("Front flip strike", input.getPlayerIndex());
                plan = new Plan()
                        .withStep(new BlindStep(new AgentOutput(), Duration.Companion.ofMillis(5)))
                        .withStep(new BlindStep(new AgentOutput().withPitch(-1).withJump(), Duration.Companion.ofMillis(5)));
                return plan.getOutput(input);
            } else {
                // Dodge to the side
                println("Side flip strike", input.getPlayerIndex());
                plan = new Plan()
                        .withStep(new BlindStep(new AgentOutput(), Duration.Companion.ofMillis(5)))
                        .withStep(new BlindStep(new AgentOutput().withSteer(correctionAngleRad < 0 ? 1 : -1).withJump(), Duration.Companion.ofMillis(5)));
                return plan.getOutput(input);
            }
        }

        double rightDirection = carToIntercept.normaliseCopy().dotProduct(car.getVelocity().normaliseCopy());
        double secondsSoFar = Duration.Companion.between(beginningOfStep, input.getTime()).getSeconds();

        if (millisTillIntercept > DODGE_TIME && secondsSoFar > 2 && rightDirection < .6 || rightDirection < 0) {
            println("Failed aerial on bad angle", input.getPlayerIndex());
            return empty();
        }

        double heightError = getHeightError(car.getVelocity(), carToIntercept, Duration.Companion.between(car.getTime(), intercept.getTime()), car);

        Vector2 flatToIntercept = carToIntercept.flatten();

        Vector2 currentFlatVelocity = car.getVelocity().flatten();

        double leftRightCorrectionAngle = currentFlatVelocity.correctionAngle(flatToIntercept);
        Vector2 desiredFlatOrientation = VectorUtil.INSTANCE
                .rotateVector(currentFlatVelocity, leftRightCorrectionAngle + signum(leftRightCorrectionAngle) * YAW_OVERCORRECT)
                .normalized();


        Vector2 currentPitchVector = getPitchVector(car.getOrientation().getNoseVector());
        double currentPitchAngle = new Vector2(1, 0).correctionAngle(currentPitchVector);
        double desiredPitchAngle = currentPitchAngle - Math.signum(heightError) * .3;
        Vector3 desiredNoseVector = convertToVector3WithPitch(desiredFlatOrientation, sin(desiredPitchAngle));

        Vector3 pitchPlaneNormal = car.getOrientation().getRightVector().crossProduct(desiredNoseVector);
        Vector3 yawPlaneNormal = VectorUtil.INSTANCE.rotateVector(desiredFlatOrientation, -Math.PI / 2).toVector3().normaliseCopy();

        Optional<AgentOutput> pitchOutput = new PitchToPlaneStep(pitchPlaneNormal).getOutput(input);
        Optional<AgentOutput> yawOutput = new YawToPlaneStep(yawPlaneNormal, false).getOutput(input);
        Optional<AgentOutput> rollOutput = new RollToPlaneStep(new Vector3(0, 0, 1), false).getOutput(input);

        return of(mergeOrientationOutputs(pitchOutput, yawOutput, rollOutput).withBoost().withJump());
    }

    public static double getDesiredVerticalAngle(Vector3 velocity, Vector3 carToIntercept) {
        Vector3 idealDirection = carToIntercept.normaliseCopy();
        Vector3 currentMotion = velocity.normaliseCopy();

        Vector2 sidescrollerCurrentVelocity = getPitchVector(currentMotion);
        Vector2 sidescrollerIdealVelocity = getPitchVector(idealDirection);

        double currentVelocityAngle = new Vector2(1, 0).correctionAngle(sidescrollerCurrentVelocity);
        double idealVelocityAngle = new Vector2(1, 0).correctionAngle(sidescrollerIdealVelocity);

        double desiredVerticalAngle = idealVelocityAngle + UPWARD_VELOCITY_MAINTENANCE_ANGLE + (idealVelocityAngle - currentVelocityAngle) * PITCH_OVERCORRECT;
        desiredVerticalAngle = min(desiredVerticalAngle, PI / 2);
        return desiredVerticalAngle;
    }

    public static double getHeightError(Vector3 velocity, Vector3 carToIntercept, Duration timeTillIntercept, CarData car) {

        double targetHeight = car.getPosition().getZ() + carToIntercept.getZ();

        double initialVelocity = velocity.getZ();

        double t = timeTillIntercept.getSeconds();
        double verticalAcceleration = EFFECTIVE_AIR_BOOST_ACCELERATION * car.getOrientation().getNoseVector().getZ() - ArenaModel.GRAVITY;

        double resultingHeight = car.getPosition().getZ() + initialVelocity * t + .5 * verticalAcceleration * t * t;

        return resultingHeight - targetHeight;
    }


    private AgentOutput mergeOrientationOutputs(Optional<AgentOutput> pitchOutput, Optional<AgentOutput> yawOutput, Optional<AgentOutput> rollOutput) {
        AgentOutput output = new AgentOutput();
        if (pitchOutput.isPresent()) {
            output.withPitch(pitchOutput.get().getPitch());
        }
        if (yawOutput.isPresent()) {
            output.withSteer(yawOutput.get().getSteer());
        }
        if (rollOutput.isPresent()) {
            output.withRoll(rollOutput.get().getRoll());
        }

        return output;
    }

    /**
     * Pretend this is suddenly a 2D sidescroller where the car can't steer, it just boosts up and down.
     * Translate into that world.
     *
     * @param unitDirection normalized vector pointing in some direction
     * @return A unit vector in two dimensions, with positive x, and z equal to unitDirection z.
     */
    private static Vector2 getPitchVector(Vector3 unitDirection) {
        return new Vector2(Math.sqrt(1 - unitDirection.getZ() * unitDirection.getZ()), unitDirection.getZ());
    }

    /**
     * Return a unit vector with the given z component, and the same flat angle as flatDirection.
     */
    private Vector3 convertToVector3WithPitch(Vector2 flat, double zComponent) {
        double xyScaler = Math.sqrt((1 - zComponent * zComponent) / (flat.getX() * flat.getX() + flat.getY() * flat.getY()));
        return new Vector3(flat.getX() * xyScaler, flat.getY() * xyScaler, zComponent);
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }

    @Override
    public String getSituation() {
        return "Finishing aerial";
    }

    @Override
    public void drawDebugInfo(Graphics2D graphics) {
        if (intercept != null) {
            ArenaDisplay.drawBall(intercept.getSpace(), graphics, new Color(23, 194, 8));
        }
    }
}
