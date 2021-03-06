package tarehart.rlbot.steps.strikes

import rlbot.render.Renderer
import tarehart.rlbot.intercept.Intercept
import tarehart.rlbot.math.Plane
import tarehart.rlbot.math.vector.Vector3
import tarehart.rlbot.physics.BallPath
import tarehart.rlbot.physics.DistancePlot
import tarehart.rlbot.rendering.RenderUtil
import tarehart.rlbot.routing.waypoint.PreKickWaypoint
import tarehart.rlbot.ui.ArenaDisplay
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D

class DirectedKickPlan (
        val intercept: Intercept,
        val ballPath: BallPath,
        val distancePlot: DistancePlot,
        val interceptModifier: Vector3,
        val desiredBallVelocity: Vector3,
        val plannedKickForce: Vector3,
        val launchPad: PreKickWaypoint,
        val easyKickAllowed: Boolean
) {

    fun drawDebugInfo(graphics: Graphics2D) {
        graphics.color = Color(73, 111, 73)
        val ball = intercept.ballSlice.space
        ArenaDisplay.drawBall(ball, graphics, graphics.color)
        graphics.stroke = BasicStroke(1f)

        graphics.color = Color(200, 0, 0)
        graphics.draw(Line2D.Double(ball.x, ball.y, ball.x + plannedKickForce.x * .4, ball.y + plannedKickForce.y * .4))

        graphics.color = Color(100, 30, 200)
        graphics.draw(Line2D.Double(ball.x, ball.y, ball.x + desiredBallVelocity.x * .4, ball.y + desiredBallVelocity.y * .4))

        graphics.color = Color(0, 0, 0)

        val (x, y) = launchPad.position
        val crossSize = 2
        graphics.draw(Line2D.Double(x - crossSize, y - crossSize, x + crossSize, y + crossSize))
        graphics.draw(Line2D.Double(x - crossSize, y + crossSize, x + crossSize, y - crossSize))
    }

    fun renderDebugInfo(renderer: Renderer) {
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, launchPad.position.toVector3()), 2.0, Color.WHITE)
        RenderUtil.drawSquare(renderer, Plane(Vector3.UP, launchPad.position.toVector3()), 1.0, Color.WHITE)

        RenderUtil.drawImpact(renderer, intercept.space, plannedKickForce.scaled(0.1), Color(1.0f, 0.4f, 0.4f))
        RenderUtil.drawBallPath(renderer, ballPath, intercept.time, RenderUtil.STANDARD_BALL_PATH_COLOR)
    }
}
