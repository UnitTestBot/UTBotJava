package org.utbot.examples.taint.alias;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/*
 * @description 最简单的数据流传递过程。
 *
 * @cwe 89
 * @bad bad
 * @good good
 * @tool fortify: SQL Injection;secbrella: SecS_SQL_Injection
 * @author 董镇山 d00305016
 */
public class CWE_89_SQL_Injection_console__env_execute_551 {

    public void bad() {
        AliasA a = new AliasA();
        a.getF().setData(System.getenv("data"));

        AliasA b = a;

        badSink(b.getF().getData());
    }

    private void badSink(String data) {
        try (Connection dbConnection = DriverManager.getConnection("url://127.0.0.1:8080");
             Statement statement = dbConnection.createStatement()) {
                    /* POTENTIAL FLAW: data concatenated into SQL statement used in execute(), which could result in SQL
                    Injection */
            Boolean result = statement.execute(
                    "insert into system (status) values ('updated') where name='" + data + "'");

            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

