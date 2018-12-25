import tornadofx.*
import javafx.collections.ObservableList
import javafx.scene.control.TextField
import java.sql.Connection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager


class CategoryModel : ItemViewModel<Category>() {
    val name = bind(Category::name)
    val description = bind(Category::description)
}


class DBController : Controller() {
    val categories: ObservableList<CategoryModel> by lazy {
        transaction {
            Category.all().map {
                CategoryModel().apply {
                    item = it
                }
            }.observable()
        }
    }

    init {
        Database.connect("jdbc:sqlite:file:data.sqlite", driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }

    fun deleteCategory(model: CategoryModel) {
        transaction {
            model.item.delete()
        }
        categories.remove(model)
    }

    fun addCategory(name: String, description: String) {
        transaction {
            val category = Category.new {
                this.name = name
                this.description = description
            }
            categories.add(
                CategoryModel().apply {
                    item = category
                })
        }
    }
}


class CategoryEditor : View("Categories") {
    val dbController: DBController by inject()
    var categoryTable: TableViewEditModel<CategoryModel> by singleAssign()
    var categories: ObservableList<CategoryModel> by singleAssign()

    var nameField: TextField by singleAssign()
    var descriptionField: TextField by singleAssign()

    override val root = borderpane {
        categories = dbController.categories

        center = vbox {
            buttonbar {
                button("DELETE SELECTED") {
                    action {
                        val model = categoryTable.tableView.selectedItem
                        when (model) {
                            null -> return@action
                            else -> dbController.deleteCategory(model)
                        }
                    }
                }
            }
            tableview<CategoryModel> {
                categoryTable = editModel
                items = categories

                enableCellEditing()
                enableDirtyTracking()

                column("Name", CategoryModel::name).makeEditable()
                column("Description", CategoryModel::description).makeEditable()
            }
        }
        right = form {
            fieldset {
                field("Name") {
                    textfield {
                        nameField = this
                    }
                }
            }
            fieldset {
                field("Description") {
                    textfield {
                        descriptionField = this
                    }
                }
            }
            button("ADD CATEGORY") {
                action {
                    dbController.addCategory(nameField.text, descriptionField.text)
                    nameField.text = ""
                    descriptionField.text = ""
                }
            }
        }
    }
}

class Kuizzy : App(CategoryEditor::class)


fun main(args: Array<String>) {
    launch<Kuizzy>(args)
}