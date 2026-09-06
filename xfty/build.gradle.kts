description = "XFTY — declarative test data factory for Java: relationships, context-aware values, persistence seam"

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
