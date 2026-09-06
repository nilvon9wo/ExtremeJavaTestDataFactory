description = "XFTY persistence binding for JPA — a real, database-backed PersistenceGateway via EntityManager"

dependencies {
    api(project(":xfty"))
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")

    testImplementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    testImplementation("org.hibernate.orm:hibernate-core:6.5.3.Final")
    testImplementation("com.h2database:h2:2.3.232")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")

    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testRuntimeOnly("org.postgresql:postgresql:42.7.4")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "xfty-jpa"
            pom {
                name.set("XFTY for JPA")
                description.set(project.description)
            }
        }
    }
}
