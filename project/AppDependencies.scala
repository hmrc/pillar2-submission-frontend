import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.6.0"
  private val hmrcMongoVersion = "2.3.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30" % "11.8.0",
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "org.typelevel"         %% "cats-core"                  % "2.13.0",
    "org.apache.xmlgraphics" % "fop"                        % "2.10",
    "commons-io"             % "commons-io"                 % "2.18.0",
    "com.beachape"          %% "enumeratum-play-json"       % "1.8.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"  % hmrcMongoVersion,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "7.0.1",
    "io.github.wolfendale"   %% "scalacheck-gen-regexp"    % "1.1.0",
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % bootstrapVersion,
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.64.8",
    "org.jsoup"               % "jsoup"                    % "1.18.3",
    "org.seleniumhq.selenium" % "selenium-java"            % "4.27.0",
    "org.seleniumhq.selenium" % "htmlunit-driver"          % "4.13.0",
    "com.danielasfregola"    %% "random-data-generator"    % "2.9",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.37"
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
