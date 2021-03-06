package tarehart.rlbot.integration.metrics

import tarehart.rlbot.time.Duration

class DistanceMetric(distance: Double, name: String = "Distance") : IntegrationMetric<Double, Double>(name, distance) {

    override fun quantify(): Double {
        return value
    }

}