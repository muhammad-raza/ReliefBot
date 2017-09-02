package tarehart.rlbot;

import tarehart.rlbot.math.SpaceTime;
import tarehart.rlbot.math.SpaceTimeVelocity;
import tarehart.rlbot.physics.ArenaModel;
import tarehart.rlbot.physics.BallPath;
import tarehart.rlbot.planning.Plan;
import tarehart.rlbot.planning.SetPieces;
import tarehart.rlbot.steps.ChaseBallStep;
import tarehart.rlbot.steps.GetBoostStep;
import tarehart.rlbot.steps.GetOnDefenseStep;
import tarehart.rlbot.steps.GetOnOffenseStep;
import tarehart.rlbot.tuning.BallRecorder;
import tarehart.rlbot.tuning.BotLog;
import tarehart.rlbot.tuning.Telemetry;
import tarehart.rlbot.ui.Readout;

import javax.swing.*;
import java.time.Duration;
import java.util.Optional;

public class Bot {

    private final Team team;
    Plan currentPlan = null;
    private Readout readout;

    private ArenaModel arenaModel;

    public enum Team {
        BLUE,
        ORANGE
    }

    public Bot(Team team) {
        this.team = team;
        readout = new Readout();
        launchReadout();
        arenaModel = new ArenaModel();
    }


    public AgentOutput processInput(AgentInput input) {

        // Just for now, always calculate ballpath so we can learn some stuff.
        BallPath ballPath = arenaModel.simulateBall(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity), Duration.ofSeconds(5));
        Telemetry.forTeam(input.team).setBallPath(ballPath);

        //BallRecorder.recordPosition(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity));
        //Optional<SpaceTimeVelocity> afterBounce = ballPath.getMotionAfterWallBounce(1);
        // Just for data gathering / debugging.
        //afterBounce.ifPresent(stv -> BallRecorder.startRecording(new SpaceTimeVelocity(input.ballPosition, input.time, input.ballVelocity), stv.getTime().plusSeconds(1)));


        AgentOutput output = getOutput(input);
        Plan.Posture posture = currentPlan != null ? currentPlan.getPosture() : Plan.Posture.NEUTRAL;
        String situation = currentPlan != null ? currentPlan.getSituation() : "";
        readout.update(input, posture, situation, BotLog.collect(input.team), Telemetry.forTeam(input.team).getBallPath());
        Telemetry.forTeam(team).reset();
        return output;
    }

    private AgentOutput getOutput(AgentInput input) {
        if (GetOnDefenseStep.needDefense(input) && (currentPlan == null || currentPlan.getPosture() != Plan.Posture.DEFENSIVE)) {
            BotLog.println("Going on defense", input.team);
            currentPlan = new Plan(Plan.Posture.DEFENSIVE).withStep(new GetOnDefenseStep());
            currentPlan.begin();
        }

        if (currentPlan == null || currentPlan.isComplete()) {
            BotLog.println("Making fresh plans", input.team);
            if (input.getMyBoost() < 30 && input.getMyPosition().distance(input.ballPosition) > 80) {
                currentPlan = new Plan().withStep(new GetBoostStep());
                currentPlan.begin();
            } else if (GetOnDefenseStep.getWrongSidedness(input) > 0) {
                BotLog.println("Getting behind the ball", input.team);
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new GetOnOffenseStep()).withStep(new ChaseBallStep());
                currentPlan.begin();
            } else {
                currentPlan = new Plan(Plan.Posture.OFFENSIVE).withStep(new ChaseBallStep());
                currentPlan.begin();
            }
        }

        if (currentPlan != null) {
            if (currentPlan.isComplete()) {
                currentPlan = null;
            } else {
                return currentPlan.getOutput(input);
            }
        }

        return new AgentOutput();
    }


    private void launchReadout() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Readout - " + team.name());
        frame.setContentPane(readout.getRootPanel());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
