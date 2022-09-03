package lt.dvim.rbr

import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.scene.control.TextInputDialog
import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.scene.layout.GridPane
import scalafx.scene.control.Label
import scalafx.scene.control.MultipleSelectionModel
import scalafx.stage.StageStyle
import scalafx.scene.control.ListView
import javafx.collections.FXCollections
import scala.concurrent.ExecutionContext
import scalafx.scene.control.SelectionModel
import scalafx.scene.control.SelectionMode

object ScalaFXHelloWorld extends JFXApp3 {

  object Style {
    val Valid = """
                    |-fx-border-color: green;
                    |-fx-border-width: 2;
                    |-fx-border-radius: 6;
    """.stripMargin

    val Invalid = """
                    |-fx-border-color: red;
                    |-fx-border-width: 2;
                    |-fx-border-radius: 6;
    """.stripMargin
  }

  override def start(): Unit = {
    given ExecutionContext = ExecutionContext.global

    val token = new TextField { promptText = "token" }
    token.text.onChange { (_, _, newValue) =>
      newValue match {
        case s"secret_$rest" if rest.length > 0 => token.style = Style.Valid
        case _                                  => token.style = Style.Invalid
      }
    }

    val notion = new TextField { promptText = "uuid" }
    notion.text.onChange { (_, _, newValue) =>
      newValue match {
        case uuid if uuid.length == 32 => notion.style = Style.Valid
        case _                         => notion.style = Style.Invalid
      }
    }

    val tagItems = FXCollections.observableArrayList[String]()
    val tagsList = new ListView[String] {}
    tagsList.getSelectionModel.setSelectionMode(SelectionMode.Multiple)
    tagsList.setItems(tagItems)

    val logItems = FXCollections.observableArrayList[String]()
    val logList = new ListView[String] {}
    logList.setItems(logItems)

    val fetchTags = new Button("Fetch tags") {
      onAction = _ => {
        logItems.add("Loading Notion page...")
        tags(token.text.get, notion.text.get).map {
          case Right(tags) =>
            tagItems.clear()
            tags.foreach(tagItems.add)
          case Left(error) =>
            logItems.add(s"ERROR: ${error.getMessage}")
        }
      }
    }
    val write = new Button("Write")

    stage = new JFXApp3.PrimaryStage {
      title = "RBR Favorites Sync"
      resizable = false
      minWidth = 400
      scene = new Scene {
        root = new HBox {
          width_=(400)
          padding = Insets(10, 10, 10, 10)
          children = Seq(
            new GridPane() {
              hgap = 10
              vgap = 10
              padding = Insets(20, 10, 10, 10)

              add(new Label("Token:"), columnIndex = 0, rowIndex = 0)
              add(token, columnIndex = 1, rowIndex = 0, colspan = 2, rowspan = 1)

              add(new Label("Notion page:"), 0, 1)
              add(notion, columnIndex = 1, rowIndex = 1, colspan = 2, rowspan = 1)

              add(tagsList, columnIndex = 0, rowIndex = 2, colspan = 3, rowspan = 1)

              add(fetchTags, columnIndex = 0, rowIndex = 3)
              add(write, columnIndex = 1, rowIndex = 3)

              add(logList, columnIndex = 0, rowIndex = 4, colspan = 3, rowspan = 1)
            }
          )
        }
      }
    }
  }
}
