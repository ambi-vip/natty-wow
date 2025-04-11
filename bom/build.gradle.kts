@Suppress("UNCHECKED_CAST")
val libraryProjects = rootProject.ext.get("libraryProjects") as Iterable<Project>

dependencies {
    constraints {
        libraryProjects.forEach {
            api(it)
        }
    }
}
