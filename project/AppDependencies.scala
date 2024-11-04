import play.core.PlayVersion
import sbt._

object AppDependencies {

  private val bootstrapVersion  = "8.4.0"
  private val hmrcMongoVersion  = "1.7.0"
  private val jsoupVersion      = "1.12.1"
  private val scalaCheckVersion = "1.17.0"
  private val seleniumVersion   = "4.4.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30" % "9.0.0",
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "org.typelevel"         %% "cats-core"                  % "2.11.0",
    "org.apache.xmlgraphics" % "fop"                        % "2.7"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"  % hmrcMongoVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0",
    "io.github.wolfendale"   %% "scalacheck-gen-regexp"    % "0.1.3",
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % bootstrapVersion,
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.62.2",
    "org.jsoup"               % "jsoup"                    % jsoupVersion,
    "org.seleniumhq.selenium" % "selenium-java"            % seleniumVersion,
    "org.seleniumhq.selenium" % "htmlunit-driver"          % "3.64.0",
    "com.danielasfregola"    %% "random-data-generator"    % "2.9",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.29"
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
