pluginManagement {
    includeBuild("../platform/cdf-plugin")
}

rootProject.name = "priceproviderservice"

include("commons")
project(":commons").projectDir = file("../platform/commons")

include("generated")

include("priceproviderservice")
