@file:Suppress("FunctionName")

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.ktor.server.cio.CIO as CIOServer

object MyTable : IntIdTable("MyTable") {
    val myColumn1 = varchar("column1", 255)
    val myColumn2 = integer("column2")
    val myColumn3 = bool("myColumn3")
}

data class MyTableRow(
    val myColumn1: String,
    val myColumn2: Int,
    val myColumn3: Boolean
) {
    override fun toString() = "$myColumn1,$myColumn2,$myColumn3"
}

fun main() {

    val db = Database.connect("jdbc:sqlite:mydatabase.db")

    transaction(Connection.TRANSACTION_SERIALIZABLE, 2, db) {
        SchemaUtils.createMissingTablesAndColumns(MyTable)
    }

    val server = embeddedServer(CIOServer, port = 8080) {
        routing {
            get("mioserver") {
                call.respondText("stocazzo")
            }

            get("time") {
                call.respondText(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            }

            route("db/{id}") {
                post {
                    val id = call.parameters["id"]?.toInt() ?: error("no id provided")
                    // stringa,stringa,stringa
                    val (col1, col2, col3) = call.receiveText().split(",")
                    transaction(Connection.TRANSACTION_SERIALIZABLE, 2, db) {
                        MyTable.insert {
                            it[MyTable.id] = id
                            it[myColumn1] = col1
                            it[myColumn2] = col2.toInt()
                            it[myColumn3] = col3.toBoolean()
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
                get {
                    val id = call.parameters["id"]?.toInt() ?: error("no id provided")
                    val myRow = transaction(Connection.TRANSACTION_SERIALIZABLE, 2, db) {
                        val row = MyTable.select { MyTable.id eq id }.singleOrNull()
                        if (row != null) MyTableRow(
                            myColumn1 = row[MyTable.myColumn1],
                            myColumn2 = row[MyTable.myColumn2],
                            myColumn3 = row[MyTable.myColumn3],
                        ) else null
                    }
                    if (myRow != null) call.respondText(myRow.toString()) else call.respond(HttpStatusCode.NotFound)
                }
            }

        }
    }

    server.start()

    val client = HttpClient(CIO)

    Window(size = IntSize(800, 600), onDismissRequest = { server.stop(0, 0) }) {

        var click by remember { mutableStateOf(0) }

        MyTheme {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { click++ }, modifier = Modifier.padding(4.dp)) {
                        Text("Clicks $click")
                    }
                    Button(onClick = { click = 0 }, modifier = Modifier.padding(4.dp)) {
                        Text("Reset")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {

                    val scope = rememberCoroutineScope()
                    var ipAddress: String? by remember { mutableStateOf(null) }

                    Button(onClick = { scope.launch { ipAddress = client.getIpAddress() } }) {
                        Text("Recupera indirizzo ip")
                    }

                    Text(ipAddress ?: "Non ancora recuperato")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {

                    val scope = rememberCoroutineScope()
                    var stocazzo: String? by remember { mutableStateOf(null) }

                    Button(onClick = { scope.launch { stocazzo = client.getStocazzo() } }) {
                        Text("Recupera stocazzo")
                    }

                    Text(stocazzo ?: "Non ancora recuperato")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                    val scope = rememberCoroutineScope()
                    var currentTime: String? by remember { mutableStateOf(null) }

                    Button(onClick = { scope.launch { currentTime = client.getTime() } }) {
                        Text("Recupera ora attuale")
                    }

                    Text(currentTime ?: "Non ancora recuperata")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {

                    val scope = rememberCoroutineScope()
                    var response: HttpResponse? by remember { mutableStateOf(null) }

                    var text by remember { mutableStateOf("") }

                    TextField(value = text, onValueChange = { text = it }, placeholder = { Text("id,col1,col2,col3") })

                    Button(onClick = {
                        val (id, col1, col2, col3) = text.split(",")
                        scope.launch { response = client.setTableRow(id.toInt(), MyTableRow(col1, col2.toInt(), col3.toBoolean())) }
                    }) {
                        Text("Salva riga")
                    }

                    Text(response?.status?.toString() ?: "Non ancora salvato")
                }
            }
        }
    }
}

@Composable
fun MyTheme(content: @Composable () -> Unit) = MaterialTheme(
    colors = lightColors(
        primary = Color(0xFFDD0D3C),
        primaryVariant = Color(0xFFC20029),
        secondary = Color.White,
        error = Color(0xFFD00036)
    ),
    content = content
)


suspend fun HttpClient.getIpAddress() =
    get<String>("http://checkip.amazonaws.com/")

suspend fun HttpClient.getStocazzo() =
    get<String>("http://localhost:8080/mioserver")

suspend fun HttpClient.getTime() =
    get<String>("http://localhost:8080/time")

suspend fun HttpClient.getTableRow(id: Int) =
    get<String>("http://localhost:8080/db/$id").split(",").let { (col1, col2, col3) ->
        MyTableRow(col1, col2.toInt(), col3.toBoolean())
    }

suspend fun HttpClient.setTableRow(id: Int, row: MyTableRow) =
    post<HttpResponse>("http://localhost:8080/db/$id") {
        body = row.toString()
    }