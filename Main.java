package carsharing;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {
        String dbFileName = null;
        try {
            for (int i = 0; i < args.length; i++) {
                if ("-databaseFileName".equals(args[i])) {
                    dbFileName = args[i + 1];
                }
            }
            try (Connection cn = initConnection(dbFileName)) {
                Controller controller = new Controller(cn);
                Command command = controller::mainMenu;
                while (command != null) {
                    command = command.exec();
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static Connection initConnection(String name) throws Exception {
        final String dbPath = "./src/carsharing/db";
        final String dbFileName = dbPath + File.separator + ((name == null || name.length() == 0) ? "carsharing" : name);
        Class.forName("org.h2.Driver");
        Connection cn = DriverManager.getConnection ("jdbc:h2:" + dbFileName);
        cn.setAutoCommit(true);
        initDb(cn);
        return cn;
    }

    private static void initDb(Connection cn) {
        final String companyTableQuery = "create table if not exists company " +
                "(id int auto_increment primary key, name varchar not null unique);";
        final String carTableQuery = "create table if not exists car " +
                "(id int auto_increment primary key, name varchar not null unique, company_id int not null, " +
                "constraint fk_company foreign key (company_id) references company (id));";
        final String customerTableQuery = "create table if not exists customer " +
                "(id int auto_increment primary key, name varchar not null unique, rented_car_id int, " +
                "constraint fk_rented_car foreign key (rented_car_id) references car (id));";
        try {
            execDsl(cn, companyTableQuery);
            execDsl(cn, carTableQuery);
            execDsl(cn, customerTableQuery);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void execDsl(Connection cn, String query) throws SQLException {
        try (Statement st = cn.createStatement()){
            st.executeUpdate(query);
        }
    }
}
