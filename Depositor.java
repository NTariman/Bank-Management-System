import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class Depositor extends JFrame implements ActionListener {
    private JTextField nameField, ageField;
    private JComboBox<String> genderCombo;
    private JPasswordField pinField;
    private JButton submitButton;

    public Depositor() {
        setTitle("Depositor Information");
        setSize(350, 280);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Components
        JLabel nameLabel = new JLabel("Name:");
        JLabel genderLabel = new JLabel("Gender:");
        JLabel ageLabel = new JLabel("Age:");
        JLabel pinLabel = new JLabel("4-digit PIN:");

        nameField = new JTextField(15);
        ageField = new JTextField(2); // Only 2 digits
        pinField = new JPasswordField(4);

        // Restrict input length for age to 2 chars
        ageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (ageField.getText().length() >= 2 || !Character.isDigit(e.getKeyChar())) {
                    e.consume();
                }
            }
        });

        String[] genders = { "Male", "Female", "Other" };
        genderCombo = new JComboBox<>(genders);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(this);

        // Layout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(genderLabel, gbc);
        gbc.gridx = 1;
        panel.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(ageLabel, gbc);
        gbc.gridx = 1;
        panel.add(ageField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(pinLabel, gbc);
        gbc.gridx = 1;
        panel.add(pinField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(submitButton, gbc);

        add(panel);
    }

    private String generateUniqueId() {
        File file = new File("Depositor.txt");
        HashSet<String> existingIds = new HashSet<>();

        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    for (String part : line.split(",")) {
                        part = part.trim();
                        if (part.startsWith("ID:")) {
                            existingIds.add(part.substring(3).trim());
                        }
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }

        Random rand = new Random();
        for (int i = 0; i < 1000; i++) {
            int num = rand.nextInt(900) + 100; // 100 to 999
            String id = String.valueOf(num);
            if (hasNoRepeatedDigits(id) && !existingIds.contains(id)) {
                return id;
            }
        }
        return null;
    }

    private boolean hasNoRepeatedDigits(String id) {
        return id.charAt(0) != id.charAt(1) &&
                id.charAt(0) != id.charAt(2) &&
                id.charAt(1) != id.charAt(2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = nameField.getText().trim();
        String gender = (String) genderCombo.getSelectedItem();
        String ageStr = ageField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();

        if (name.isEmpty() || ageStr.isEmpty() || pin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!pin.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(this, "PIN must be a 4-digit number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!ageStr.matches("\\d{1,2}")) {
            JOptionPane.showMessageDialog(this, "Age must be a 1- or 2-digit number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age <= 0 || age > 99) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Age must be a positive integer (1-99).", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String id = generateUniqueId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Unable to generate unique ID.", "ID Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Save to Depositor.txt
        try (FileWriter fw = new FileWriter("Depositor.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.printf("Name: %s, ID: %s, Gender: %s, Age: %d, PIN: %s%n", name, id, gender, age, pin);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving data: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Display entered information
        String message = String.format(
                "Depositor Information:\nName: %s\nID: %s\nGender: %s\nAge: %d\nPIN: %s\n\n",
                name, id, gender, age, pin
        );
        JOptionPane.showMessageDialog(this, message, "Submission Successful", JOptionPane.INFORMATION_MESSAGE);

        // Clear fields
        nameField.setText("");
        ageField.setText("");
        pinField.setText("");
        genderCombo.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Depositor().setVisible(true);
        });
    }
}
