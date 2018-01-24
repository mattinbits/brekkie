package com.nordea.commonplatforms

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}
import ViewUtil._

class ViewUtilSpec extends WordSpec with Matchers {

  "ViewUtil" should {

    "Provide ordinal suffix" in {
      (1 to 31).map(ordinalSuffix) shouldBe
        Seq("st", "nd", "rd") ++ Seq.fill(17)("th") ++ Seq("st", "nd", "rd") ++ Seq.fill(7)("th") :+ "st"
    }

    "Render date in UI Form" in {
      Seq("2017-11-02", "2017-12-01", "2017-12-08").map(LocalDate.parse).map(uiDate) shouldBe
        Seq("Thursday 2nd November", "Friday 1st December", "Friday 8th December")
    }
  }

}
