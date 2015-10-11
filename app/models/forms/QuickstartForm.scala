package models.forms

import java.util.regex.Pattern

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

  def validateUrl(url:String): String ={
    val regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".r;
    regex findFirstIn(url) match{
      case Some(str)=> url
      case _ => throw new Exception("Invalid URL format")
    }
  }
}