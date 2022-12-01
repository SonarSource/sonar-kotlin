package checks

import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.GregorianCalendar

abstract class PreparedStatementAndResultSetCheckSample {

    private val INDEX_42 = 42
    private val INDEX_1 = 1

    abstract fun getIntValue(): Int
    abstract fun getPreparedStatement(): PreparedStatement

    fun nonCompliant(connection: Connection, date: Date, salary: Double) {
        val ps: PreparedStatement = connection.prepareStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?")
        ps.setDate(0, date) // Noncompliant {{PreparedStatement indices start at 1.}}
//                 ^
        ps.setDouble(3, salary) // Noncompliant

        val rs: ResultSet = ps.executeQuery()
        while (rs.next()) {
            rs.getString(0) // Noncompliant {{ResultSet indices start at 1.}}
//                       ^
        }
    }

    fun compliant(connection: Connection, date: Date, salary: Double) {
        val ps: PreparedStatement = connection.prepareStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?")
        ps.setDate(1, date)
        ps.setDouble(2, salary)

        val rs: ResultSet = ps.executeQuery()
        while (rs.next()) {
            rs.getString(1)
        }
    }

    fun foo(connection: Connection) {
        val ps = connection.prepareStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?")
        ps.setDate(0, Date(0)) // Noncompliant {{PreparedStatement indices start at 1.}}
        ps.setDouble(3, 0.0) // Noncompliant {{This "PreparedStatement" only has 2 parameters.}}
        ps.setString(getIntValue(), "") // Compliant - first argument can not be evaluated
        ps.setInt(1, 0) // Compliant
        val rs = ps.executeQuery()
        rs.getString(0) // Noncompliant {{ResultSet indices start at 1.}}
        rs.getDate(0, GregorianCalendar()) // Noncompliant {{ResultSet indices start at 1.}}
        rs.getString(1) // Compliant
    }

    fun dam(connection: Connection, query: String?) {
        val ps = connection.prepareStatement(query)
        ps.setDate(0, Date(0)) // Noncompliant {{PreparedStatement indices start at 1.}}
        ps.setDouble(3, 0.0) // Compliant - Query of the preparedStatement is unknown
    }

    fun cro(ps: PreparedStatement) {
        ps.setDate(0, Date(0)) // Noncompliant {{PreparedStatement indices start at 1.}}
        ps.setDouble(3, 0.0) // Compliant - Query of the preparedStatement is unknown
    }

    fun elk() {
        getPreparedStatement().setDate(0, Date(0)) // Noncompliant {{PreparedStatement indices start at 1.}}
        getPreparedStatement().setDouble(3, 0.0) // Compliant - Query of the preparedStatement is unknown
    }

    fun gra() {
        val ps = getPreparedStatement()
        ps.setDate(0, Date(0)) // Noncompliant {{PreparedStatement indices start at 1.}}
        ps.setDouble(3, 0.0) // Compliant - Query of the preparedStatement is unknown
        ps.setDate(0, Date(0)) // Noncompliant {{PreparedStatement indices start at 1.}}
        ps.setDouble(3, 0.0) // Compliant - Query of the preparedStatement is unknown
    }

    fun fromConstant(con: Connection) {
        val sql = "select ... from ... where job_id = ?"
        val ps = con.prepareStatement(sql)
        ps.setInt(INDEX_42, 1) // Noncompliant
        ps.setInt(INDEX_1, 1) // Compliant
    }

    fun concatenation1(connection: Connection) {
        var selectClause = ";"
        selectClause = "SELECT anything FROM somewhere " + selectClause
        val ps = connection.prepareStatement(selectClause)
        ps.setString(1, "anything") // Compliant - do not check var
    }

    fun concatenation2(connection: Connection) {
        var selectClause = "SELECT anything FROM ? "
        selectClause += "WHERE one = ? AND two = ?"
        val ps = connection.prepareStatement(selectClause)
        ps.setString(0, "anything") // Noncompliant
        ps.setString(1, "anything") // Compliant
        ps.setString(2, "anything") // Compliant
        ps.setString(3, "anything") // Compliant
        ps.setString(4, "anything") // Compliant - do not check var
    }

}
