package crossproject

import scala.scalajs.js.Dynamic.global

class LoggerImpl extends Logger {
  def log(s: String) = {
    val document = global.document
    val newP = document.createElement("p")
    newP.innerHTML = "s"
    document.appendChild(newP)
  }
}
