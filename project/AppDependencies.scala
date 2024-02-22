import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.3.0"
  private val hmrcMongoVersion = "1.6.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"    % "8.2.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"            % hmrcMongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.17.0",
    "org.scalatestplus"       %% "mockito-3-4"              % "3.2.10.0",
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"    %  "0.1.3",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0",
    "org.jsoup"               %  "jsoup"                    % "1.14.3",
    "com.typesafe.play"       %% "play-test"                % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"            % "1.16.42",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.62.2",
    "org.scalatestplus"       %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "com.danielasfregola"     %% "random-data-generator"    % "2.9",
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"    % "1.1.0"

  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
