package org.phonify.pages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootApplication
public class PhoneApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(PhoneApp.class)
                .headless(false)
                .run(args);

        EventQueue.invokeLater(() -> {
            // Display the login or registration dialog first
            LoginRegistrationDialog loginRegistrationDialog = new LoginRegistrationDialog(context.getBean(UserRepository.class));
            loginRegistrationDialog.setLocationRelativeTo(null);
            loginRegistrationDialog.setVisible(true);

            // If login is successful, show the PhonePanel
            if (loginRegistrationDialog.isLoginSuccessful()) {
                JFrame frame = new JFrame("Phonify");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(new PhonePanel(context.getBean(PhoneRepository.class),
                        context.getBean(OrderRepository.class), context.getBean(UserRepository.class)));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}

enum LoggedInUser {
    INSTANCE;

    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

enum SelectedPhone {
    INSTANCE;

    private Phone phone;

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }
}

class LoginRegistrationDialog extends JDialog implements ActionListener {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton registerButton;
    private final UserRepository userRepository;

    private boolean loginSuccessful = false;
    private boolean registrationSuccessful = false;

    public LoginRegistrationDialog(UserRepository userRepository) {
        this.userRepository = userRepository;
        setTitle("Login or Register");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        add(inputPanel, BorderLayout.CENTER);

        usernameField = new JTextField();
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);

        passwordField = new JPasswordField();
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.addActionListener(this);
        registerButton = new JButton("Register");
        registerButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            // Perform login logic
            if (login(username, password)) {
                JOptionPane.showMessageDialog(this, "Login successful!");

                User user = userRepository.findUserByUserNameAndUserPassword(username, password);
                LoggedInUser.INSTANCE.setUser(user);
                loginSuccessful = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password. Please try again.");
            }
        } else if (e.getSource() == registerButton) {
            // Open registration dialog
            RegistrationDialog registrationDialog = new RegistrationDialog(userRepository);
            registrationDialog.setLocationRelativeTo(this);
            registrationDialog.setVisible(true);

            if (registrationDialog.isRegistrationSuccessful()) {
                JOptionPane.showMessageDialog(this, "Registration successful! Please log in.");
                loginSuccessful = true;
                dispose();
            }
        }
    }

    private boolean login(String username, String password) {

        User user = userRepository.findUserByUserNameAndUserPassword(username, password);

        return user != null;
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    public boolean isRegistrationSuccessful() {
        return registrationSuccessful;
    }
}

class RegistrationDialog extends JDialog implements ActionListener {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField emailField;
    private final JButton registerButton;
    private final UserRepository userRepository;

    private boolean registrationSuccessful = false;

    public RegistrationDialog(UserRepository userRepository) {
        this.userRepository = userRepository;
        setTitle("Register");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        add(inputPanel, BorderLayout.CENTER);

        usernameField = new JTextField();
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);

        passwordField = new JPasswordField();
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        emailField = new JTextField();
        inputPanel.add(new JLabel("Email:"));
        inputPanel.add(emailField);

        registerButton = new JButton("Register");
        registerButton.addActionListener(this);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(registerButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == registerButton) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String email = emailField.getText();

            // Perform registration logic
            if (register(username, email, password)) {
                registrationSuccessful = true;
                dispose();
            }
        }
    }

    private boolean register(String username, String email, String password) {
        try {
            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(this, "Invalid email format", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            User user = new User(username, email, password);

            userRepository.save(user);

            LoggedInUser.INSTANCE.setUser(user);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Registration failed", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        // Regular expression for email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    public boolean isRegistrationSuccessful() {
        return registrationSuccessful;
    }
}

@Repository
interface PhoneRepository extends JpaRepository<Phone, Long> {
}

@Repository
interface UserRepository extends JpaRepository<User, Long> {

    User findUserByUserNameAndUserPassword(String username, String password);
}


@Repository
interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findOrderBySeller(User seller);
}

@Entity
@Table(name = "phones")
@Getter
@Setter
class Phone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand", length = 15, nullable = false)
    private String brand;

    @Column(name = "model", length = 50, nullable = false)
    private String model;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Override
    public String toString() {
        return brand + " " + model + " Price: $" + price + " Quantity: " + quantity;
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, updatable = false, insertable = false, nullable = false)
    private int userID;

    @Column(name = "username", length = 20, unique = true, updatable = true, insertable = true, nullable = false)
    private String userName;

    @Column(name = "user_email", length = 30, unique = true, updatable = true, insertable = true, nullable = false)
    private String userEmail;

    @Column(name = "password", length = 500, unique = false, updatable = true, insertable = true, nullable = false)
    private String userPassword;

    public User(String userName, String userEmail, String userPassword) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Username: ").append(userName).append("\n");
        sb.append("  Email: ").append(userEmail).append("\n");
        return sb.toString();
    }
}

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_address", length = 50, nullable = false)
    private String buyerAddress;

    @Column(name = "buyer_email", length = 50, nullable = false)
    private String buyerEmail;

    @Column(name = "buyer_name", length = 50, nullable = false)
    private String buyerName;

    @ManyToOne
    @JoinColumn(name = "phone_id", nullable = false)
    private Phone phone;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    public Order(String buyerAddress, String buyerEmail, String buyerName, Phone phone, User seller) {
        this.buyerAddress = buyerAddress;
        this.buyerEmail = buyerEmail;
        this.buyerName = buyerName;
        this.phone = phone;
        this.seller = seller;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Buyer Name: ").append(buyerName).append("\n");
        sb.append(" Buyer Email: ").append(buyerEmail).append("\n");
        sb.append(" Buyer Address: ").append(buyerAddress).append("\n");
        sb.append(" Phone Details: ").append(phone.getBrand()).append(" ").append(phone.getModel()).append("\n");
        sb.append(" Seller: ").append(seller.getUserName()).append("\n");
        return sb.toString();
    }
}

class PhonePanel extends JPanel implements ActionListener {
    private final JTextField brandField;
    private final JTextField quantityField;
    private final JTextField modelField;
    private final JTextField descriptionField;
    private final JTextField priceField;
    private final JButton saveButton;
    private final JButton sellButton;
    private final JButton ordersButton;
    private final JButton clearSelectionButton;
    private final PhoneRepository phoneRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DefaultListModel<Phone> phoneListModel;
    private final JList<Phone> phoneList;
    private Phone selectedPhone;

    private final JTextField searchBrandField;
    private final JTextField searchModelField;
    private final JButton searchButton;

    public PhonePanel(PhoneRepository phoneRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.phoneRepository = phoneRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.phoneListModel = new DefaultListModel<>();
        this.phoneList = new JList<>(phoneListModel);


        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS)); // Set vertical BoxLayout
        add(inputPanel, BorderLayout.CENTER);

        brandField = new JTextField(20);
        inputPanel.add(new JLabel("Brand:"));
        inputPanel.add(brandField);

        modelField = new JTextField(20);
        inputPanel.add(new JLabel("Model:"));
        inputPanel.add(modelField);

        descriptionField = new JTextField(20);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descriptionField);

        priceField = new JTextField(20);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(priceField);

        quantityField = new JTextField(20);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);

        saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        sellButton = new JButton("Sell");
        sellButton.addActionListener(this);

        clearSelectionButton = new JButton("Clear Selection");
        clearSelectionButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(sellButton);
        buttonPanel.add(clearSelectionButton);
        add(buttonPanel, BorderLayout.SOUTH);

        phoneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        phoneList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedPhone = phoneList.getSelectedValue(); // Update the selected phone
                SelectedPhone.INSTANCE.setPhone(selectedPhone);
                if (selectedPhone != null) {
                    // Populate text fields with selected phone's data
                    brandField.setText(selectedPhone.getBrand());
                    modelField.setText(selectedPhone.getModel());
                    descriptionField.setText(selectedPhone.getDescription());
                    priceField.setText(String.valueOf(selectedPhone.getPrice()));
                    quantityField.setText(String.valueOf(selectedPhone.getQuantity()));
                }
            }
        });
        add(new JScrollPane(phoneList), BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchBrandField = new JTextField(15);
        searchModelField = new JTextField(15);
        searchButton = new JButton("Search");
        searchButton.addActionListener(this);
        searchPanel.add(new JLabel("Search by Brand: "));
        searchPanel.add(searchBrandField);
        searchPanel.add(new JLabel("Model: "));
        searchPanel.add(searchModelField);
        searchPanel.add(searchButton);
        ordersButton = new JButton("Orders"); // Initializing ordersButton
        ordersButton.addActionListener(this);
        buttonPanel.add(ordersButton); // Adding the ordersButton to the buttonPanel
        add(searchPanel, BorderLayout.NORTH);

        refreshPhoneList();
    }

    private void refreshPhoneList() {
        phoneListModel.clear();
        List<Phone> phones = phoneRepository.findAll();
        phones.forEach(phoneListModel::addElement);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            if (selectedPhone != null) { // Check if a phone is selected
                // Update selected phone's data with text fields' data
                selectedPhone.setBrand(brandField.getText());
                selectedPhone.setModel(modelField.getText());
                selectedPhone.setDescription(descriptionField.getText());
                selectedPhone.setPrice(Double.parseDouble(priceField.getText()));
                selectedPhone.setQuantity(Integer.parseInt(quantityField.getText()));

                phoneRepository.save(selectedPhone); // Save the updated phone
                JOptionPane.showMessageDialog(this, "Phone updated successfully!");
                refreshPhoneList();
            } else {
                String brand = brandField.getText();
                String model = modelField.getText();
                String description = descriptionField.getText();
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());

                Phone phone = new Phone();
                phone.setBrand(brand);
                phone.setModel(model);
                phone.setDescription(description);
                phone.setPrice(price);
                phone.setQuantity(quantity);

                phoneRepository.save(phone);
                JOptionPane.showMessageDialog(this, "Phone saved successfully!");
                clearFields();
                refreshPhoneList();
            }
        } else if (e.getSource() == sellButton) {
            Phone selectedPhone = getSelectedPhone();
            if (selectedPhone != null) {
                OrderDialog dialog = new OrderDialog(selectedPhone, orderRepository, phoneRepository);
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
                if (dialog.isSellConfirmed()) {
                    selectedPhone.setQuantity(selectedPhone.getQuantity() - 1);
                    phoneRepository.save(selectedPhone);
                    refreshPhoneList();
                }
            }
            phoneList.clearSelection();
        } else if (e.getSource() == clearSelectionButton) {
            phoneList.clearSelection();
            clearFields();
            selectedPhone = null; // Reset selected phone
        } else if (e.getSource() == searchButton) {
            String brandKeyword = searchBrandField.getText().trim();
            String modelKeyword = searchModelField.getText().trim();

            List<Phone> filteredPhones = phoneRepository.findAll();

            if (!brandKeyword.isEmpty()) {
                filteredPhones = filteredPhones.stream()
                        .filter(phone -> phone.getBrand().toLowerCase().contains(brandKeyword.toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (!modelKeyword.isEmpty()) {
                filteredPhones = filteredPhones.stream()
                        .filter(phone -> phone.getModel().toLowerCase().contains(modelKeyword.toLowerCase()))
                        .collect(Collectors.toList());
            }

            phoneListModel.clear();
            filteredPhones.forEach(phoneListModel::addElement);
        } else if (e.getSource() == ordersButton) {
            showOrdersDialog(); // Show orders dialog upon button click
        }

    }

    private void showOrdersDialog() {
        // Create and show orders dialog
        OrdersDialog ordersDialog = new OrdersDialog(userRepository, orderRepository);
        ordersDialog.setLocationRelativeTo(this);
        ordersDialog.setVisible(true);
    }

    private Phone getSelectedPhone() {
        return SelectedPhone.INSTANCE.getPhone();
    }

    private void clearFields() {
        brandField.setText("");
        modelField.setText("");
        descriptionField.setText("");
        priceField.setText("");
        quantityField.setText("");
    }
}

class OrdersDialog extends JDialog {
    private final JList<Order> orderList;
    private final DefaultListModel<Order> orderListModel;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrdersDialog(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        setTitle("My Orders");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        orderListModel = new DefaultListModel<>();
        orderList = new JList<>(orderListModel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        mainPanel.add(new JScrollPane(orderList), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Load and display orders
        loadOrders();

        setSize(600, 400); // Set your desired size here
    }

    private void loadOrders() {
        User loggedInUser = LoggedInUser.INSTANCE.getUser();
        if (loggedInUser != null) {
            List<Order> userOrders = orderRepository.findOrderBySeller(loggedInUser);
            orderListModel.clear();
            userOrders.forEach(orderListModel::addElement);
        }
    }
}

class OrderDialog extends JDialog implements ActionListener {
    private final JTextField buyerNameField;
    private final JTextField buyerAddressField;
    private final JTextField buyerEmailField;
    private final JButton sellButton;
    private final Phone phone;
    private boolean sellConfirmed;

    private final OrderRepository orderRepository;

    private final PhoneRepository phoneRepository;

    public OrderDialog(Phone phone, OrderRepository orderRepository, PhoneRepository phoneRepository) {
        this.phone = phone;
        this.orderRepository = orderRepository;
        this.phoneRepository = phoneRepository;
        setTitle("Sell Phone");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        add(inputPanel, BorderLayout.CENTER);

        inputPanel.add(new JLabel("Selected Phone:"));
        inputPanel.add(new JLabel(SelectedPhone.INSTANCE.getPhone().toString()));

        buyerNameField = new JTextField();
        inputPanel.add(new JLabel("Buyer Name:"));
        inputPanel.add(buyerNameField);

        buyerAddressField = new JTextField();
        inputPanel.add(new JLabel("Buyer Address:"));
        inputPanel.add(buyerAddressField);

        buyerEmailField = new JTextField();
        inputPanel.add(new JLabel("Buyer Email:"));
        inputPanel.add(buyerEmailField);

        sellButton = new JButton("Sell");
        sellButton.addActionListener(this);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(sellButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Optional<Phone> searchedPhone = phoneRepository.findById(phone.getId());

        Phone phone1 = searchedPhone.get();

        if (e.getSource() == sellButton && phone1.getQuantity() > 0) {
            String buyerName = buyerNameField.getText();
            String buyerAddress = buyerAddressField.getText();
            String buyerEmail = buyerEmailField.getText();

            if (buyerName.isEmpty() || buyerAddress.isEmpty() || buyerEmail.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.");
                return;
            }

            Order order = new Order();
            order.setPhone(SelectedPhone.INSTANCE.getPhone());
            order.setBuyerName(buyerName);
            order.setBuyerAddress(buyerAddress);
            order.setBuyerEmail(buyerEmail);
            order.setSeller(LoggedInUser.INSTANCE.getUser());

            orderRepository.save(order);

            sellConfirmed = true;

            JOptionPane.showMessageDialog(this, "Phone sold successfully!");
            dispose();
        } else {
            JOptionPane.showMessageDialog(null, "Cannot Sell Phone. No Quantity!", "Message", JOptionPane.INFORMATION_MESSAGE);

        }
    }

    public boolean isSellConfirmed() {
        return sellConfirmed;
    }
}