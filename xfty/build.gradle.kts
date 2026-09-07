description = "XFTY — declarative test data factory for Java: relationships, context-aware values, persistence seam"

dependencies {
    testImplementation("org.mockito:mockito-core:5.14.2")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "xfty"
            pom {
                name.set("XFTY")
                description.set(project.description)
            }
        }
    }
}
