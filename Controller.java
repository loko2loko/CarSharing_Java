package carsharing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Controller {
    private final Connection connection;

    public Controller(Connection connection) {
        this.connection = connection;
    }

    public Command mainMenu() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Log in as a manager", this::managerMenu));
        menu.put(2, new MenuCommand("Log in as a customer", this::customerLoginMenu));
        menu.put(3, new MenuCommand("Create a customer", this::createCustomer));
        menu.put(0, new MenuCommand("Exit", null));
        return displayMenu(menu);
    }

    public Command managerMenu() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Company list", this::getCompanyList));
        menu.put(2, new MenuCommand("Create a company", this::createCompany));
        menu.put(0, new MenuCommand("Back", this::mainMenu));
        return displayMenu(menu);
    }

    public Command companyMenu(Company company) {
        System.out.println("'" + company.getName() + "' company");
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Car list", () -> getCarList(company)));
        menu.put(2, new MenuCommand("Create a car", () -> createCar(company)));
        menu.put(0, new MenuCommand("Back", this::managerMenu));
        return displayMenu(menu);
    }

    public Command displayMenu(Map<Integer, MenuCommand> menu) {
        menu.entrySet().stream()
                .map(i -> i.getKey() + ". " + i.getValue().getCaption())
                .forEach(System.out::println);
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        int key = in.nextInt();
        if (menu.containsKey(key)) {
            return menu.get(key).getCommand();
        }
        return null;
    }

    public Command getCompanyList() {
        System.out.println("Company list:");
        final String query = "select * from company order by id;";
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (!rs.next()) {
                System.out.println("The company list is empty!");
                return this::managerMenu;
            } else {
                do {
                    Company company = new Company(rs.getInt("id"), rs.getString("name"));
                    menu.put(company.getId(), new MenuCommand(company.getName(), () -> this.companyMenu(company)));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", this::managerMenu));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    public Command createCompany() {
        System.out.println("Enter the company name:");
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        final String query = "insert into company (name) values (?);";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setString(1, name);
            st.executeUpdate();
            System.out.println("The company was created!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return this::managerMenu;
    }

    public Command getCarList(Company company) {
        System.out.println("Car list:");
        final String query = "select * from car where company_id = ? order by id;";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setInt(1, company.getId());
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                System.out.println("The car list is empty!");
            } else {
                int i = 1;
                do {
                    System.out.println(i + ". " + rs.getString("name"));
                    i++;
                } while (rs.next());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> companyMenu(company);
    }

    public Command createCar(Company company) {
        System.out.println("Enter the car name:");
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        final String query = "insert into car (name, company_id) values (?, ?);";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setString(1, name);
            st.setInt(2, company.getId());
            st.executeUpdate();
            System.out.println("The car was added!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> companyMenu(company);
    }

    public Command createCustomer() {
        System.out.println("Enter the customer name:");
        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        String name = in.nextLine();
        final String query = "insert into customer (name) values (?);";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setString(1, name);
            st.executeUpdate();
            System.out.println("The customer was added!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return this::mainMenu;
    }

    public Command customerLoginMenu() {
        System.out.println("Choose a customer:");
        final String query = "select * from customer order by id;";
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (!rs.next()) {
                System.out.println("The customer list is empty!");
                return this::mainMenu;
            } else {
                do {
                    Customer customer = new Customer(rs.getInt("id"), rs.getString("name"), rs.getObject("rented_car_id", Integer.class));
                    menu.put(customer.getId(), new MenuCommand(customer.getName(), () -> this.customerMenu(customer)));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", this::mainMenu));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    public Command customerMenu(Customer customer) {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Rent a car", () -> rentCar(customer)));
        menu.put(2, new MenuCommand("Return a rented car", () -> returnCar(customer)));
        menu.put(3, new MenuCommand("My rented car", () -> myRentedCar(customer)));
        menu.put(0, new MenuCommand("Back", this::mainMenu));
        return displayMenu(menu);
    }

    public Command rentCar(Customer customer) {
        if (customer.getRentedCarId() != null) {
            System.out.println("You've already rented a car!");
            return () -> customerMenu(customer);
        }

        System.out.println("Choose a company:");
        final String companyQuery = "select * from company order by id;";
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(companyQuery);
            if (!rs.next()) {
                System.out.println("The company list is empty!");
                return () -> customerMenu(customer);
            } else {
                do {
                    Company company = new Company(rs.getInt("id"), rs.getString("name"));
                    menu.put(company.getId(), new MenuCommand(company.getName(), () -> chooseCar(customer, company)));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", () -> customerMenu(customer)));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    private Command chooseCar(Customer customer, Company company) {
        System.out.println("Choose a car:");
        final String carQuery = "select car.id, car.name from car left join customer on car.id = customer.rented_car_id " +
                "where car.company_id = ? and customer.id is null order by car.id;";
        try (PreparedStatement st = connection.prepareStatement(carQuery)) {
            st.setInt(1, company.getId());
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                System.out.println("No available cars in the '" + company.getName() + "' company.");
                return () -> rentCar(customer);
            } else {
                Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
                int i = 1;
                do {
                    int carId = rs.getInt("id");
                    String carName = rs.getString("name");
                    menu.put(i, new MenuCommand(carName, () -> rentCar(customer, carId, carName)));
                    i++;
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", () -> rentCar(customer)));
                return displayMenu(menu);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> customerMenu(customer);
    }

    private Command rentCar(Customer customer, int carId, String carName) {
        final String updateQuery = "update customer set rented_car_id = ? where id = ?;";
        try (PreparedStatement st = connection.prepareStatement(updateQuery)) {
            st.setInt(1, carId);
            st.setInt(2, customer.getId());
            st.executeUpdate();
            System.out.println("You rented '" + carName + "'");
            customer.setRentedCarId(carId);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> customerMenu(customer);
    }

    public Command returnCar(Customer customer) {
        if (customer.getRentedCarId() == null) {
            System.out.println("You didn't rent a car!");
        } else {
            final String updateQuery = "update customer set rented_car_id = null where id = ?;";
            try (PreparedStatement st = connection.prepareStatement(updateQuery)) {
                st.setInt(1, customer.getId());
                st.executeUpdate();
                System.out.println("You've returned a rented car!");
                customer.setRentedCarId(null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return () -> customerMenu(customer);
    }

    public Command myRentedCar(Customer customer) {
        if (customer.getRentedCarId() == null) {
            System.out.println("You didn't rent a car!");
        } else {
            final String query = "select car.name, company.name as company_name from car " +
                    "join company on car.company_id = company.id " +
                    "where car.id = ?;";
            try (PreparedStatement st = connection.prepareStatement(query)) {
                st.setInt(1, customer.getRentedCarId());
                ResultSet rs = st.executeQuery();
                if (rs.next()) {
                    System.out.println("Your rented car:");
                    System.out.println(rs.getString("name"));
                    System.out.println("Company:");
                    System.out.println(rs.getString("company_name"));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return () -> customerMenu(customer);
    }
}
