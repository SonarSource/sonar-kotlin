// Pins org.sonarqube to a version without the sonarResolver bug SCANGRADLE-293.
beforeSettings {
    pluginManagement {
        resolutionStrategy {
            eachPlugin {
                if (requested.id.id == "org.sonarqube") {
                    useVersion("7.3.1.8318")
                }
            }
        }
    }
}
