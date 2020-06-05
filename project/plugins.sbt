val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).filter(_.nonEmpty).getOrElse("1.0.0")
addSbtPlugin("org.scala-js"        % "sbt-scalajs"     % scalaJSVersion)
addSbtPlugin("com.jsuereth"        % "sbt-pgp"         % "1.1.2")
addSbtPlugin("com.eed3si9n"        % "sbt-unidoc"      % "0.4.3")
addSbtPlugin("pl.project13.scala"  % "sbt-jmh"         % "0.3.7")
addSbtPlugin("com.typesafe"        % "sbt-mima-plugin" % "0.7.0")
addSbtPlugin("com.typesafe.sbt"    % "sbt-git"         % "1.0.0")
addSbtPlugin("org.xerial.sbt"      % "sbt-sonatype"    % "3.9.2")
addSbtPlugin("de.heikoseeberger"   % "sbt-header"      % "5.6.0")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"    % "2.4.0")
addSbtPlugin("com.github.tkawachi" % "sbt-doctest"     % "0.9.6")
addSbtPlugin("org.scalameta"       % "sbt-mdoc"        % "2.2.1")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.2")
