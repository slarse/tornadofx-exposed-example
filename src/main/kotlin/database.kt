import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object Categories : IntIdTable() {
    val name = varchar("name", 64).uniqueIndex()
    val description = varchar("description", 128)
}

class Category(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Category>(Categories)

    var name by Categories.name
    var description by Categories.description

    override fun toString(): String {
        return "Category(name=\"$name\", description=\"$description\")"
    }
}

fun main(args: Array<String>) {
    // "connect" a database file called data.sqlite in the current working directory
    // (creates the file if id does not exist)
    Database.connect("jdbc:sqlite:file:data.sqlite", driver = "org.sqlite.JDBC")
    // this isolation level is required for sqlite, may not be applicable to other DBMS
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        // create the table
        SchemaUtils.create(Categories)

        // add some entries
        Category.new {
            name = "java"
            description = "The Java programming language"
        }

        Category.new {
            name = "cpp"
            description = "The C++ programming language"
        }
    }

    // new transaction to check the results
    transaction {
        Category.all().forEach { println(it) }
    }
}
