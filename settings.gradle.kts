plugins {
    // Bundles every module's publication and uploads it to the Maven Central
    // Portal in one deployment. See docs/publishing.md.
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

rootProject.name = "xfty-parent"

include("xfty")
include("xfty-jpa")

nmcpSettings {
    centralPortal {
        username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
        password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
        // Upload as a draft deployment; a human reviews and releases it in the
        // portal. Switch to "AUTOMATIC" once the pipeline has proven itself.
        publishingType = "USER_MANAGED"
    }
}
