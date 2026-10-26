addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.12")

// Fingerprints assets so that `Assets.versioned` URLs can be cached forever
addSbtPlugin("com.github.sbt" % "sbt-digest" % "2.1.0")
