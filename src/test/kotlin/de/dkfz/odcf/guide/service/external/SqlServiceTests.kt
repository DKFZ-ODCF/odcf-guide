package de.dkfz.odcf.guide.service.external

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.dkfz.odcf.guide.service.implementation.external.SqlServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.slf4j.LoggerFactory
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.sql.Connection
import java.sql.DriverManager

@ExtendWith(SpringExtension::class)
class SqlServiceTests {

    private lateinit var connection: Connection

    @InjectMocks
    lateinit var sqlServiceTestsMock: SqlServiceImpl

    private fun initListAppender(): ListAppender<ILoggingEvent> {
        val fooLogger = LoggerFactory.getLogger(SqlServiceImpl::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        fooLogger.addAppender(listAppender)

        return listAppender
    }

    @BeforeEach
    fun setup() {
        connection = DriverManager.getConnection("jdbc:h2:mem:test")
        val statement = connection.createStatement()
        statement.execute("CREATE TABLE IF NOT EXISTS test (id INT PRIMARY KEY, name VARCHAR(255))")
        statement.execute("INSERT INTO test VALUES (1, 'test')")
        statement.execute("INSERT INTO test VALUES (2, 'test2')")
        statement.execute("INSERT INTO test VALUES (3, null)")
    }

    @Test
    fun `test get from remote`() {
        val sql = "SELECT * FROM test"

        val result = sqlServiceTestsMock.getFromRemote(connection, "name", sql)

        assertThat(result).hasSize(2)
        assertThat(result).contains("test")
        assertThat(result).contains("test2")
    }

    @Test
    fun `test get from remote with error`() {
        val sql = "SELECT ' FROM test"
        val listAppender = initListAppender()

        val result = sqlServiceTestsMock.getFromRemote(connection, "name", sql)
        val logsList = listAppender.list

        assertThat(result).hasSize(0)
        assertThat(logsList).hasSize(1)
    }

    @Test
    fun `test get multiple from remote`() {
        val sql = "SELECT * FROM test"

        val result = sqlServiceTestsMock.getMultipleFromRemote(connection, listOf("name"), sql)

        assertThat(result).hasSize(3)
        assertThat(result.map { it["name"] }).contains("test")
        assertThat(result.map { it["name"] }).contains("test2")
        assertThat(result.map { it["name"] }).contains("")
    }

    @Test
    fun `test get multiple from remote with error`() {
        val sql = "SELECT ' FROM test"
        val listAppender = initListAppender()

        val result = sqlServiceTestsMock.getMultipleFromRemote(connection, listOf("name"), sql)
        val logsList = listAppender.list

        assertThat(result).hasSize(0)
        assertThat(logsList).hasSize(1)
    }

    @Test
    fun `test get set from remote`() {
        val sql = "SELECT * FROM test"

        val result = sqlServiceTestsMock.getSetFromRemote(connection, "name", sql)

        assertThat(result).hasSize(2)
        assertThat(result).isInstanceOf(Set::class.java)
        assertThat(result).contains("test")
        assertThat(result).contains("test2")
    }
}
