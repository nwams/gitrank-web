package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the sign up process.
 */
object FeedbackForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "scoreDocumentation" -> number,
      "scoreMaturity" -> number,
      "scoreDesign" -> number,
      "scoreSupport" -> number,
      "feedback" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(
                   scoreDocumentation: Int,
                   scoreMaturity: Int,
                   scoreDesign: Int,
                   scoreSupport: Int,
                   feedback: String)
}