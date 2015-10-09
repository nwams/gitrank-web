package models.forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the sign up process.
 */
object QuickstartForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "title" -> nonEmptyText,
      "descritpion" -> nonEmptyText,
      "url" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(
                   title: String,
                   description: String,
                   url: String)
}