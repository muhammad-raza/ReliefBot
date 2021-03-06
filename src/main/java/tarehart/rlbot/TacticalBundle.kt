package tarehart.rlbot

import tarehart.rlbot.planning.TeamPlan
import tarehart.rlbot.planning.ZonePlan
import tarehart.rlbot.tactics.TacticalSituation

class TacticalBundle(val agentInput: AgentInput, val tacticalSituation: TacticalSituation, val teamPlan: TeamPlan? = null, val zonePlan: ZonePlan? = null) {
    companion object {
        fun dummy(input: AgentInput): TacticalBundle {
            return TacticalBundle(input, TacticalSituation.DUMMY)
        }
    }
}