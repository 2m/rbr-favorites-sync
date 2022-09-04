/*
 * Copyright 2022 github.com/2m/rbr-favorites-sync
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

package lt.dvim.rbr

import java.io.File
import org.ini4j.Reg
import org.ini4j.Wini
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.sys.process._
import scala.util.Try
import scalafx.application.JFXApp3
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.control.ListView
import scalafx.scene.control.MultipleSelectionModel
import scalafx.scene.control.PasswordField
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.SelectionModel
import scalafx.scene.control.TextField
import scalafx.scene.control.TextInputDialog
import scalafx.scene.effect.DropShadow
import scalafx.scene.layout.ColumnConstraints
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.scene.layout.VBox
import scalafx.scene.paint._
import scalafx.scene.paint.Color._
import scalafx.scene.text.Text
import scalafx.stage.StageStyle

object RbrFavoritesSyncScalaFx extends JFXApp3 {

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

    val storageFile = {
      val file = new File("rbr-favorites-sync.ini")
      file.createNewFile()
      file
    }
    val localStorage = new Wini(storageFile)

    val favoritesFile = {
      val path = Try(
        Process("""reg query "HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Rallysimfans RBR" /v InstallPath""").!!
      ).toEither.fold(
        _ => "",
        output => {
          val installPath = output.split("REG_SZ").toList.last.trim
          s"$installPath\\rsfdata\\cache\\"
        }
      )
      val file = new File(s"${path}favorites.ini")
      file.createNewFile()
      file
    }
    val favoritesStorage = new Wini(favoritesFile)

    val token = new PasswordField { promptText = "token" }
    token.text.onChange { (_, _, newValue) =>
      newValue match {
        case s"secret_$rest" if rest.length > 0 =>
          token.style = Style.Valid
          localStorage.put("config", "token", newValue)
          localStorage.store()
        case _ => token.style = Style.Invalid
      }
    }
    token.text = Option(localStorage.get("config", "token")).getOrElse("")

    val notion = new TextField { promptText = "uuid" }
    notion.text.onChange { (_, _, newValue) =>
      newValue match {
        case uuid if uuid.length == 32 =>
          notion.style = Style.Valid
          localStorage.put("config", "notion", newValue)
          localStorage.store()
        case _ => notion.style = Style.Invalid
      }
    }
    notion.text = Option(localStorage.get("config", "notion")).getOrElse("")

    val tagItems = ObservableBuffer.empty[String]
    val tagsList = new ListView[String] {
      items = tagItems
    }
    tagsList.getSelectionModel.setSelectionMode(SelectionMode.Multiple)

    val logItems = ObservableBuffer.from(List("Welcome to RBR Favorites Sync"))
    val logList = new ListView[String] {
      items = logItems
      prefHeight = 150
    }

    logItems.prepend {
      val favorites = favoritesFile.getAbsolutePath
      s"Parsing favorites from $favorites"
    }
    logItems.prepend {
      val stageCount = Option(favoritesStorage.get("FavoriteStages")).map(_.size).getOrElse(0)
      s"Found $stageCount favorite stages"
    }

    var loadedStages = List.empty[Stage]

    val fetchTags = new Button("Fetch tags") {
      onAction = _ => {
        logItems.prepend("Loading Notion page...")
        stages(token.text.get, notion.text.get).map {
          case Right(stages) =>
            loadedStages = stages

            val tags = stages.flatMap(_.tags).toSet.toList.sorted

            tagItems.clear()
            tags.foreach(tagItems.add)

            Platform.runLater {
              logItems.prepend {
                val stageCount = stages.size
                val tagCount = tags.size
                s"Loaded $stageCount stages with $tagCount unique tags"
              }
            }
          case Left(error) =>
            Platform.runLater {
              logItems.prepend(s"ERROR: ${error.getMessage}")
            }
        }
      }
    }
    val write = new Button("Write") {
      onAction = _ => {
        val selectedTags = tagsList.getSelectionModel.getSelectedItems.asScala.toList
        val selectedStages = loadedStages.filter(_.tags.intersect(selectedTags).nonEmpty)

        Option(favoritesStorage.get("FavoriteStages")).map(_.clear())
        selectedStages.foreach { stage =>
          favoritesStorage.put("FavoriteStages", stage.id.toString, "f");
        }
        favoritesStorage.store()

        logItems.prepend(s"Marked ${selectedStages.size} stages as favorites")
      }
    }

    val neverGrow = new ColumnConstraints()
    neverGrow.setHgrow(Priority.Never)

    val alwaysGrow = new ColumnConstraints()
    alwaysGrow.setHgrow(Priority.Always)

    val grid = new GridPane() {
      gridLinesVisible = false
      hgap = 10
      vgap = 10
      padding = Insets(20, 10, 10, 10)
      columnConstraints.addAll(neverGrow, neverGrow, alwaysGrow)

      add(new Label("Token:"), columnIndex = 0, rowIndex = 0)
      add(token, columnIndex = 1, rowIndex = 0, colspan = 2, rowspan = 1)

      add(new Label("Notion page:"), 0, 1)
      add(notion, columnIndex = 1, rowIndex = 1, colspan = 2, rowspan = 1)

      add(tagsList, columnIndex = 0, rowIndex = 2, colspan = 3, rowspan = 1)

      add(fetchTags, columnIndex = 0, rowIndex = 3)
      add(write, columnIndex = 1, rowIndex = 3)

      add(logList, columnIndex = 0, rowIndex = 4, colspan = 3, rowspan = 1)
    }

    stage = new JFXApp3.PrimaryStage {
      title = "RBR Favorites Sync"
      resizable = false
      minWidth = 400
      scene = new Scene {
        root = new HBox {
          padding = Insets(10, 10, 10, 10)
          children = Seq(grid)
        }
      }
    }
  }
}
