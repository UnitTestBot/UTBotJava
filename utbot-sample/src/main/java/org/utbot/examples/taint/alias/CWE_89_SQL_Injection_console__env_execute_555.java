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
public class CWE_89_SQL_Injection_console__env_execute_555 {

    public void bad() {
        AliasB b1 = new AliasB();
        b1.setData("data");

        AliasB b2 = new AliasB();
        b2.setData(System.getenv("data"));

        AliasA a1 = new AliasA(b1);
        AliasA a2 = new AliasA(b2);

        AliasB b3 = a1.getF();
        AliasB b4 = a2.getF();

        badSink(b3.getData());
        badSink(b4.getData());
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

