package tarehart.rlbot.physics;

import mikera.vectorz.Vector2;
import mikera.vectorz.Vector3;
import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BallPath {

    ArrayList<SpaceTimeVelocity> path = new ArrayList<>();

    public BallPath(SpaceTimeVelocity start) {
        path.add(start);
    }

    public void addSlice(SpaceTimeVelocity spaceTime) {
        path.add(spaceTime);
    }

    public List<SpaceTimeVelocity> getSlices() {
        return path;
    }

    public Optional<SpaceTimeVelocity> getMotionAt(LocalDateTime time) {
        if (time.isBefore(path.get(0).getTime()) || time.isAfter(path.get(path.size() - 1).getTime())) {
            return Optional.empty();
        }

        for (int i = 0; i < path.size() - 1; i++) {
            SpaceTimeVelocity current = path.get(i);
            SpaceTimeVelocity next = path.get(i + 1);
            if (next.getTime().isAfter(time)) {

                long simulationStepMillis = Duration.between(current.getTime(), next.getTime()).toMillis();
                double tweenPoint = Duration.between(current.getTime(), time).toMillis() * 1.0 / simulationStepMillis;
                Vector3 toNext = (Vector3) next.getSpace().subCopy(current.getSpace());
                Vector3 toTween = (Vector3) toNext.scaleCopy(tweenPoint);
                Vector3 space = current.getSpace().addCopy(toTween);
                Vector3 velocity = averageVectors(current.getVelocity(), next.getVelocity(), 1 - tweenPoint);
                return Optional.of(new SpaceTimeVelocity(new SpaceTime(space, time), velocity));
            }
        }

        return Optional.of(getEndpoint());
    }

    private Vector3 averageVectors(Vector3 a, Vector3 b, double weightOfA) {
        Vector3 average = (Vector3) a.scaleCopy(weightOfA);
        average.add(b.scaleCopy((1-weightOfA)));
        return average;
    }

    /**
     * Bounce counting starts at 1.
     *
     * 0 is not a valid input.
     */
    public Optional<SpaceTimeVelocity> getMotionAfterWallBounce(int targetBounce) {

        assert targetBounce > 0;

        Vector3 previousVelocity = null;
        int numBounces = 0;

        for (int i = 1; i < path.size(); i++) {
            SpaceTimeVelocity spt = path.get(i);
            SpaceTimeVelocity previous = path.get(i - 1);

            if (isWallBounce(previous.getVelocity(), spt.getVelocity())) {
                numBounces++;
            }

            if (numBounces == targetBounce) {
                if (path.size() == i + 1) {
                    return Optional.empty();
                }
                return Optional.of(spt.copy());
            }
        }

        return Optional.empty();
    }

    private boolean isWallBounce(Vector3 previousVelocity, Vector3 currentVelocity) {
        if (currentVelocity.magnitudeSquared() < .01) {
            return false;
        }
        Vector2 prev = new Vector2(previousVelocity.x, previousVelocity.y);
        Vector2 curr = new Vector2(currentVelocity.x, currentVelocity.y);

        if (curr.magnitude() / prev.magnitude() < 0.5) {
            return true; //
        }

        prev.normalise();
        curr.normalise();

        return prev.dotProduct(curr) < .95;
    }

    private boolean isFloorBounce(Vector3 previousVelocity, Vector3 currentVelocity) {
        return previousVelocity.z < 0 && currentVelocity.z > 0;
    }

    private Vector3 getVelocity(SpaceTime before, SpaceTime after) {
        long millisBetween = Duration.between(before.time, after.time).toMillis();
        double secondsBetween = millisBetween / 1000.0;
        Vector3 prevToNext = (Vector3) after.space.subCopy(before.space);
        return (Vector3) prevToNext.scaleCopy(1 / secondsBetween);
    }

    public SpaceTimeVelocity getStartPoint() {
        return path.get(0).copy();
    }

    public SpaceTimeVelocity getEndpoint() {
        return path.get(path.size() - 1).copy();
    }

    public Optional<SpaceTimeVelocity> getLanding(LocalDateTime startOfSearch) {

        for (int i = 1; i < path.size(); i++) {
            SpaceTimeVelocity spt = path.get(i);

            if (spt.getTime().isBefore(startOfSearch)) {
                continue;
            }

            SpaceTimeVelocity previous = path.get(i - 1);


            if (isFloorBounce(previous.getVelocity(), spt.getVelocity())) {
                if (path.size() == i + 1) {
                    return Optional.empty();
                }

                double floorGapOfPrev = previous.getSpace().z - ArenaModel.BALL_RADIUS;
                double floorGapOfCurrent = spt.getSpace().z - ArenaModel.BALL_RADIUS;

                SpaceTimeVelocity bouncePosition = new SpaceTimeVelocity(new Vector3(spt.getSpace().x, spt.getSpace().y, ArenaModel.BALL_RADIUS), spt.getTime(), spt.getVelocity());
                if (floorGapOfPrev < floorGapOfCurrent) {
                    // TODO: consider interpolating instead of just picking the more accurate.
                    bouncePosition = new SpaceTimeVelocity(
                            new Vector3(previous.getSpace().x, previous.getSpace().y, ArenaModel.BALL_RADIUS),
                            previous.getTime(),
                            spt.getVelocity());
                }

                return Optional.of(bouncePosition);
            }
        }

        return Optional.empty();
    }
}