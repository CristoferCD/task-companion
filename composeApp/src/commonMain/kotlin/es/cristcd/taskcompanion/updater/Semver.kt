package es.cristcd.taskcompanion.updater

data class Semver(val major: Int, val minor: Int, val patch: Int, val prerelease: String?, val build: String?)

operator fun Semver.compareTo(other: Semver): Int {
    if (major != other.major) {
        return major.compareTo(other.major)
    }
    if (minor != other.minor) {
        return minor.compareTo(other.minor)
    }
    if (patch != other.patch) {
        return patch.compareTo(other.patch)
    }
    //TODO: handle prerelease and build
    return 0
}

fun String.toSemver(): Semver? {
    val regex = "(?<MAJOR>(?:0|(?:[1-9]\\d*)))\\.(?<MINOR>(?:0|(?:[1-9]\\d*)))\\.(?<PATCH>(?:0|(?:[1-9]\\d*)))(?:-(?<prerelease>[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*))?(?:\\+(?<build>[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*))?".toRegex()
    val match = regex.matchEntire(this) ?: return null

    val major = match.groups["MAJOR"]?.value?.toInt() ?: return null
    val minor = match.groups["MINOR"]?.value?.toInt() ?: return null
    val patch = match.groups["PATCH"]?.value?.toInt() ?: return null

    val prerelease = match.groups["prerelease"]?.value
    val build = match.groups["build"]?.value

    return Semver(major, minor, patch, prerelease, build)
}