import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class LoginPage extends JFrame implements ActionListener {
    private JTextField userField;
    private JPasswordField pinField;
    private JButton loginButton, registerButton;

    // Map to store Name and PIN (loaded from Depositor.txt)
    private Map<String, String> userPinMap = new HashMap<>();
    // Map to store Name and Balance (loaded from Balance.txt)
    private Map<String, Double> userBalanceMap = new HashMap<>();
    // Map to store Name and Status (from Depositor.txt)
    private Map<String, String> userStatusMap = new HashMap<>();

    public LoginPage() {
        setTitle("Login Page");
        setSize(400, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load credentials from Depositor.txt
        loadCredentials();
        // Load balances from Balance.txt (create if doesn't exist)
        loadBalances();

        // Create components
        JLabel userLabel = new JLabel("User:");
        JLabel pinLabel = new JLabel("PIN:");

        userField = new JTextField(15);
        pinField = new JPasswordField(10);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        loginButton.addActionListener(this);
        registerButton.addActionListener(this);

        // Layout setup
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        panel.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(pinLabel, gbc);

        gbc.gridx = 1;
        panel.add(pinField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(loginButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(registerButton, gbc);

        add(panel);
    }

    // Loads Name, PIN, and Status from Depositor.txt
    private void loadCredentials() {
        File file = new File("Depositor.txt");
        if (!file.exists()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String name = "";
                String pin = "";
                String status = "Enabled"; // Default status

                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("Name:")) {
                        name = part.substring(5).trim();
                    } else if (part.startsWith("PIN:")) {
                        pin = part.substring(4).trim();
                    } else if (part.startsWith("Status:")) {
                        status = part.substring(7).trim();
                    }
                }
                if (!name.isEmpty() && !pin.isEmpty()) {
                    userPinMap.put(name, pin);
                    userStatusMap.put(name, status);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading Depositor.txt: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Loads user balances from Balance.txt or initializes to 0 if not found
    private void loadBalances() {
        File file = new File("Balance.txt");
        if (!file.exists()) {
            for (String user : userPinMap.keySet()) {
                userBalanceMap.put(user, 0.0);
            }
            saveAllBalances();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String name = "";
                double balance = 0.0;
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("Name:")) {
                        name = part.substring(5).trim();
                    } else if (part.startsWith("Balance:")) {
                        try {
                            balance = Double.parseDouble(part.substring(8).trim());
                        } catch (NumberFormatException e) {
                            balance = 0.0;
                        }
                    }
                }
                if (!name.isEmpty()) {
                    userBalanceMap.put(name, balance);
                }
            }
            for (String user : userPinMap.keySet()) {
                if (!userBalanceMap.containsKey(user)) {
                    userBalanceMap.put(user, 0.0);
                }
            }
            saveAllBalances();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading Balance.txt: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Saves all balances to Balance.txt
    private void saveAllBalances() {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("Balance.txt")))) {
            for (Map.Entry<String, Double> entry : userBalanceMap.entrySet()) {
                out.printf("Name: %s, Balance: %.2f%n", entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            // Ignore error for now
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton) {
            String user = userField.getText().trim();
            String pin = String.valueOf(pinField.getPassword()).trim();

            if (user.isEmpty() || pin.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both User and PIN!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (userPinMap.containsKey(user) && userPinMap.get(user).equals(pin)) {
                // Always reload credentials to get the latest status (in case Admin changed status)
                reloadUserStatus(user);

                // Check if account is enabled
                String status = userStatusMap.getOrDefault(user, "Enabled");
                if ("Disabled".equalsIgnoreCase(status)) {
                    JOptionPane.showMessageDialog(this, "Your account is currently DISABLED. Please contact the administrator.", "Account Disabled", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double balance = userBalanceMap.getOrDefault(user, 0.0);
                new AccountGUI(user, balance, this).setVisible(true);
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid User or PIN!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == registerButton) {
            // Open the Depositor registration window (assumes Depositor.java is in the same project)
            SwingUtilities.invokeLater(() -> {
                new Depositor().setVisible(true);
            });
        }
    }

    // Updates the balance in the map and saves to file
    public void updateBalance(String user, double newBalance) {
        userBalanceMap.put(user, newBalance);
        saveAllBalances();
    }

    // Records transaction for monitoring (append to file)
    public void recordTransaction(String user, String type, double amount, double balance) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("TransactionLog.txt", true)))) {
            out.printf("%s,%s,%.2f,%.2f,%s%n", user, type, amount, balance, new Date());
        } catch (IOException e) {
            // Ignore error for now
        }
    }

    // Records transfer for both sender and recipient
    public void recordTransfer(String sender, String recipient, double amount, double senderBalance, double recipientBalance) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("TransactionLog.txt", true)))) {
            String dateStr = new Date().toString();
            out.printf("%s,Transfer Out,%.2f,%.2f,To: %s,%s%n", sender, amount, senderBalance, recipient, dateStr);
            out.printf("%s,Transfer In,%.2f,%.2f,From: %s,%s%n", recipient, amount, recipientBalance, sender, dateStr);
        } catch (IOException e) {
            // Ignore error for now
        }
    }

    // Reloads the user's status from Depositor.txt for real-time admin changes
    private void reloadUserStatus(String user) {
        File file = new File("Depositor.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String foundStatus = null;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String name = "";
                String status = "Enabled";
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("Name:")) {
                        name = part.substring(5).trim();
                    } else if (part.startsWith("Status:")) {
                        status = part.substring(7).trim();
                    }
                }
                if (name.equals(user)) {
                    foundStatus = status;
                    break;
                }
            }
            if (foundStatus != null) {
                userStatusMap.put(user, foundStatus);
            }
        } catch (IOException e) {
            // Ignore error for now
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginPage().setVisible(true);
        });
    }

    // Inner class for the Account GUI
    class AccountGUI extends JFrame implements ActionListener {
        private String user;
        private double balance;
        private LoginPage parent;
        private JButton depositButton, withdrawButton, checkBalanceButton, logoutButton, transferButton;

        public AccountGUI(String user, double balance, LoginPage parent) {
            this.user = user;
            this.balance = balance;
            this.parent = parent;

            setTitle(user + " - Account");
            setSize(750, 120);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            depositButton = new JButton("Deposit");
            withdrawButton = new JButton("Withdraw");
            checkBalanceButton = new JButton("Check Balance");
            transferButton = new JButton("Transfer");
            logoutButton = new JButton("Logout");

            depositButton.addActionListener(this);
            withdrawButton.addActionListener(this);
            checkBalanceButton.addActionListener(this);
            transferButton.addActionListener(this);
            logoutButton.addActionListener(this);

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(depositButton);
            buttonPanel.add(withdrawButton);
            buttonPanel.add(checkBalanceButton);
            buttonPanel.add(transferButton);
            buttonPanel.add(logoutButton);

            setLayout(new BorderLayout());
            add(buttonPanel, BorderLayout.SOUTH);
        }

        @Override // 4. Polymorphism - Overriding actionPerformed from ActionListener interface
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == depositButton) {
                String input = JOptionPane.showInputDialog(this, "Enter deposit amount (₱):", "Deposit", JOptionPane.PLAIN_MESSAGE);
                if (input != null && !input.trim().isEmpty()) {
                    try {
                        double amount = Double.parseDouble(input.trim());
                        if (amount <= 0) throw new NumberFormatException();
                        balance += amount;
                        parent.updateBalance(user, balance);
                        parent.recordTransaction(user, "Deposit", amount, balance);
                        JOptionPane.showMessageDialog(this, "Deposit successful!");
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid amount!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == withdrawButton) {
                String input = JOptionPane.showInputDialog(this, "Enter withdraw amount (₱):", "Withdraw", JOptionPane.PLAIN_MESSAGE);
                if (input != null && !input.trim().isEmpty()) {
                    try {
                        double amount = Double.parseDouble(input.trim());
                        if (amount <= 0) throw new NumberFormatException();
                        if (amount > balance) {
                            JOptionPane.showMessageDialog(this, "Insufficient balance!", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            balance -= amount;
                            parent.updateBalance(user, balance);
                            parent.recordTransaction(user, "Withdraw", amount, balance);
                            JOptionPane.showMessageDialog(this, "Withdrawal successful!");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid amount!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (e.getSource() == checkBalanceButton) {
                JOptionPane.showMessageDialog(this, "Current Balance: \u20B1" + String.format("%.2f", balance));
            } else if (e.getSource() == transferButton) {
                handleTransfer();
            } else if (e.getSource() == logoutButton) {
                setVisible(false);
                parent.setVisible(true);
            }
        }

        // Bank transfer logic
        private void handleTransfer() {
            JPanel transferPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            JTextField toUserField = new JTextField();
            JTextField amountField = new JTextField();
            transferPanel.add(new JLabel("Recipient Name:"));
            transferPanel.add(toUserField);
            transferPanel.add(new JLabel("Transfer Amount (₱):"));
            transferPanel.add(amountField);

            int result = JOptionPane.showConfirmDialog(this, transferPanel, "Bank Transfer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String toUser = toUserField.getText().trim();
                String amountStr = amountField.getText().trim();

                if (toUser.isEmpty() || amountStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please provide both recipient and amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (toUser.equalsIgnoreCase(user)) {
                    JOptionPane.showMessageDialog(this, "You cannot transfer to yourself!", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!parent.userPinMap.containsKey(toUser)) {
                    JOptionPane.showMessageDialog(this, "Recipient does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String status = parent.userStatusMap.getOrDefault(toUser, "Enabled");
                if ("Disabled".equalsIgnoreCase(status)) {
                    JOptionPane.showMessageDialog(this, "Recipient's account is DISABLED.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    double transferAmount = Double.parseDouble(amountStr);
                    if (transferAmount <= 0) {
                        JOptionPane.showMessageDialog(this, "Amount must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (transferAmount > balance) {
                        JOptionPane.showMessageDialog(this, "Insufficient balance!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Perform transfer
                    double toUserBalance = parent.userBalanceMap.getOrDefault(toUser, 0.0);
                    balance -= transferAmount;
                    toUserBalance += transferAmount;

                    parent.updateBalance(user, balance);
                    parent.updateBalance(toUser, toUserBalance);
                    parent.recordTransfer(user, toUser, transferAmount, balance, toUserBalance);

                    JOptionPane.showMessageDialog(this, "Transfer successful!\nYou sent ₱" + String.format("%.2f", transferAmount) + " to " + toUser + ".");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid amount!", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
