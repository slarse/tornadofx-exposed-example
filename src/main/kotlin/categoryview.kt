import tornadofx.*
import javafx.collections.ObservableList
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
}


class CategoryEditor : View("Categories") {
    val dbController: DBController by inject()
    var categoryTable: TableViewEditModel<CategoryModel> by singleAssign()
    var categories: ObservableList<CategoryModel> by singleAssign()

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

                column("Name", CategoryModel::name)
                column("Description", CategoryModel::description)
            }
        }
    }
}

class Kuizzy : App(CategoryEditor::class)


fun main(args: Array<String>) {
    launch<Kuizzy>(args)
}