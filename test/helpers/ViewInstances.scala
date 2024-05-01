/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers

import play.api.i18n.DefaultLangs
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.config._
import uk.gov.hmrc.hmrcfrontend.views.config.{HmrcFooterItems, StandardBetaBanner}
import uk.gov.hmrc.hmrcfrontend.views.html.components._
import uk.gov.hmrc.hmrcfrontend.views.html.helpers._
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.components.gds._
import views.html.templates._

trait ViewInstances extends Configs with StubMessageControllerComponents {

  val hmrcTrackingConsent = new HmrcTrackingConsentSnippet(new TrackingConsentConfig(configuration))

  val accessibilityConfiguration = new AccessibilityStatementConfig(configuration)

  lazy val tudorCrownConfig: TudorCrownConfig = TudorCrownConfig(configuration)

  val govukHeader = new GovukHeader(tudorCrownConfig)

  val govukTemplate = new GovukTemplate(govukHeader, new GovukFooter, new GovukSkipLink, new FixedWidthPageLayout)

  val hmrcStandardHeader = new HmrcStandardHeader(
    hmrcHeader = new HmrcHeader(
      hmrcBanner = new HmrcBanner(tudorCrownConfig),
      hmrcUserResearchBanner = new HmrcUserResearchBanner(),
      govukPhaseBanner = new GovukPhaseBanner(govukTag = new GovukTag()),
      tudorCrownConfig = tudorCrownConfig
    )
  )
  val hmrcStandardFooter = new HmrcStandardFooter(
    new HmrcFooter(govukFooter = new GovukFooter),
    new HmrcFooterItems(new AccessibilityStatementConfig(configuration))
  )

  val hmrcNewTabLink = new HmrcNewTabLink

  val assetsConfig = new AssetsConfig()

  val hmrcScripts        = new HmrcScripts(assetsConfig)
  val hmrcTimeoutDilogue = new HmrcTimeoutDialog

  val languageUtils = new LanguageUtils(new DefaultLangs(), configuration)

  val govukHint                = new GovukHint
  val govukRadios              = new GovukRadios(new GovukErrorMessage, new GovukFieldset, new GovukHint, new GovukLabel)
  val govukInput               = new GovukInput(new GovukErrorMessage, new GovukHint, new GovukLabel)
  val govukDateInput           = new GovukDateInput(new GovukErrorMessage, new GovukHint, new GovukFieldset, govukInput)
  val govukCheckboxes          = new GovukCheckboxes(new GovukErrorMessage, new GovukFieldset, new GovukHint, new GovukLabel)
  val govukLabel               = new GovukLabel()
  val govukDetails             = new GovukDetails
  val govukPanel               = new GovukPanel
  val govukTable               = new GovukTable
  val govukButton              = new GovukButton
  val govukFieldSet            = new GovukFieldset
  val govukErrorSummary        = new GovukErrorSummary
  val govukErrorMessage        = new GovukErrorMessage
  val govukSummaryList         = new GovukSummaryList
  val govukSelect              = new GovukSelect(new GovukErrorMessage, new GovukHint, new GovukLabel)
  val govukBackLink            = new GovukBackLink
  val govukWarningText         = new GovukWarningText
  val formWithCSRF             = new FormWithCSRF
  val heading                  = new Heading
  val h2                       = new HeadingH2
  val paragraphBody            = new ParagraphBody
  val paragraphBodyLink        = new ParagraphBodyLink
  val span                     = new Span
  val paragraphMessageWithLink = new ParagraphMessageWithLink
  val sectionHeader            = new SectionHeader

  val hmrcPageHeading = new HmrcPageHeading
  val govUkInsetText  = new GovukInsetText

  val govukNotificationBanner = new GovukNotificationBanner()

  val govukLayout = new GovukLayout(
    govukTemplate = govukTemplate,
    govukHeader = govukHeader,
    govukFooter = new GovukFooter,
    govukBackLink = govukBackLink,
    defaultMainContentLayout = new TwoThirdsMainContent,
    fixedWidthPageLayout = new FixedWidthPageLayout
  )

  val pillar2layout = new Layout(
    govukLayout,
    new GovukBackLink,
    new HmrcHead(hmrcTrackingConsent, assetsConfig),
    hmrcStandardHeader,
    hmrcStandardFooter,
    hmrcTrackingConsent,
    new HmrcLanguageSelect(),
    hmrcTimeoutDilogue,
    new HmrcReportTechnicalIssueHelper(new HmrcReportTechnicalIssue(), new ContactFrontendConfig(configuration)),
    hmrcScripts,
    new StandardBetaBanner,
    new Stylesheets
  )

}
