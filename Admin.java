import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class Admin extends JFrame implements ActionListener {
    private JTable depositorTable;
    private JScrollPane scrollPane;
    private JButton enableButton, disableButton, deleteButton, refreshButton, monitorButton;
    private JTextField searchField;
    private JLabel searchLabel;
    private List<String[]> depositors;
    private String[] columnNames = {"Name", "ID", "Gender", "Age", "PIN", "Status"};
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public Admin() {
        setTitle("Admin - List of Depositors");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load depositors from Depositor.txt
        depositors = loadDepositors();

        // Convert list to 2D array for JTable
        String[][] tableData = depositors.toArray(new String[0][]);

        model = new DefaultTableModel(tableData, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table not editable
            }
        };

        depositorTable = new JTable(model);
        depositorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // TableRowSorter for filtering
        sorter = new TableRowSorter<>(model);
        depositorTable.setRowSorter(sorter);

        scrollPane = new JScrollPane(depositorTable);

        enableButton = new JButton("Enable Account");
        disableButton = new JButton("Disable Account");
        deleteButton = new JButton("Delete Account");
        refreshButton = new JButton("Refresh");
        monitorButton = new JButton("Monitor");

        enableButton.addActionListener(this);
        disableButton.addActionListener(this);
        deleteButton.addActionListener(this);
        refreshButton.addActionListener(this);
        monitorButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(enableButton);
        buttonPanel.add(disableButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(monitorButton);

        // Search bar
        searchLabel = new JLabel("Search Name: ");
        searchField = new JTextField(18);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        // Search as you type
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                if (text.length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)^" + Pattern.quote(text), 0));
                }
            }
        });

        // Layout
        setLayout(new BorderLayout());
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private List<String[]> loadDepositors() {
        List<String[]> data = new ArrayList<>();
        File file = new File("Depositor.txt");
        if (!file.exists()) {
            return data;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] row = new String[6];
                String[] parts = line.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("Name:")) {
                        row[0] = part.substring(5).trim();
                    } else if (part.startsWith("ID:")) {
                        row[1] = part.substring(3).trim();
                    } else if (part.startsWith("Gender:")) {
                        row[2] = part.substring(7).trim();
                    } else if (part.startsWith("Age:")) {
                        row[3] = part.substring(4).trim();
                    } else if (part.startsWith("PIN:")) {
                        row[4] = part.substring(4).trim();
                    } else if (part.startsWith("Status:")) {
                        row[5] = part.substring(7).trim();
                    }
                }
                if (row[5] == null || row[5].isEmpty()) {
                    row[5] = "Enabled";
                }
                data.add(row);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading Depositor.txt: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
        return data;
    }

    // Save depositors with status back to Depositor.txt
    private void saveDepositors(List<String[]> depositors) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("Depositor.txt")))) {
            for (String[] row : depositors) {
                out.printf("Name: %s, ID: %s, Gender: %s, Age: %s, PIN: %s, Status: %s%n",
                        row[0], row[1], row[2], row[3], row[4], row[5]);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving Depositor.txt: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteFromBalance(String name) {
        File file = new File("Balance.txt");
        if (!file.exists()) return;
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                if (!line.trim().startsWith("Name: " + name + ",")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) { /* ignore */ }
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            for (String line : lines) out.println(line);
        } catch (IOException e) { /* ignore */ }
    }

    private void deleteFromTransactionLog(String name) {
        File file = new File("TransactionLog.txt");
        if (!file.exists()) return;
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                if (!line.trim().startsWith(name + ",")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) { /* ignore */ }
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            for (String line : lines) out.println(line);
        } catch (IOException e) { /* ignore */ }
    }

    private void showMonitor(String name) {
        new MonitorWindow(name).setVisible(true);
    }

    // Monitor window for transaction/history remains the same
    class MonitorWindow extends JFrame {
        public MonitorWindow(String user) {
            setTitle(user + " - Monitoring Window");
            setSize(1000, 400);
            setLocationRelativeTo(null);

            String status = null;
            for (String[] row : depositors) {
                if (row[0].equals(user)) {
                    status = row[5];
                    break;
                }
            }
            if (status == null) status = "Enabled";

            String[] columnNames = { "No.", "Type", "Amount (₱)", "Total Deposited (₱)", "Total Withdrawn (₱)", "Balance (₱)", "Timestamp" };
            List<Object[]> rowData = new ArrayList<>();

            double totalDeposited = 0.0;
            double totalWithdrawn = 0.0;
            int count = 1;

            try (BufferedReader br = new BufferedReader(new FileReader("TransactionLog.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", 5);
                    if (parts.length < 5) continue;
                    if (!parts[0].trim().equals(user)) continue;
                    String type = parts[1].trim();
                    double amount = Double.parseDouble(parts[2].trim());
                    double balance = Double.parseDouble(parts[3].trim());
                    String timestamp = parts[4].trim();

                    if (type.equals("Deposit")) {
                        totalDeposited += amount;
                    } else if (type.equals("Withdraw")) {
                        totalWithdrawn += amount;
                    }

                    rowData.add(new Object[]{
                            count++,
                            type,
                            String.format("%.2f", amount),
                            String.format("%.2f", totalDeposited),
                            String.format("%.2f", totalWithdrawn),
                            String.format("%.2f", balance),
                            timestamp
                    });
                }
            } catch (FileNotFoundException ex) {
            } catch (IOException ex) {
            }

            Object[][] tableData = rowData.toArray(new Object[0][]);
            JTable table = new JTable(tableData, columnNames);
            JScrollPane scrollPane = new JScrollPane(table);

            JLabel statusLabel = new JLabel();
            statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            if ("Disabled".equalsIgnoreCase(status)) {
                statusLabel.setText("Account Status: DISABLED");
                statusLabel.setForeground(Color.RED);
            } else {
                statusLabel.setText("Account Status: ENABLED");
                statusLabel.setForeground(new Color(0, 128, 0));
            }

            setLayout(new BorderLayout());
            add(statusLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }
    }

    // Handle enable/disable/delete/refresh/monitor button actions
    @Override
    public void actionPerformed(ActionEvent e) {
        int selectedRow = depositorTable.getSelectedRow();

        if (e.getSource() == enableButton || e.getSource() == disableButton) {
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an account to " + (e.getSource() == enableButton ? "enable" : "disable") + ".", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String[] row = depositors.get(depositorTable.convertRowIndexToModel(selectedRow));
            if (e.getSource() == enableButton) {
                if ("Enabled".equalsIgnoreCase(row[5])) {
                    JOptionPane.showMessageDialog(this, "Account is already enabled.");
                    return;
                }
                row[5] = "Enabled";
            } else {
                if ("Disabled".equalsIgnoreCase(row[5])) {
                    JOptionPane.showMessageDialog(this, "Account is already disabled.");
                    return;
                }
                row[5] = "Disabled";
            }
            saveDepositors(depositors);
            model.setValueAt(row[5], depositorTable.convertRowIndexToModel(selectedRow), 5);
            JOptionPane.showMessageDialog(this, "Account status updated.");
        } else if (e.getSource() == deleteButton) {
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an account to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = depositorTable.convertRowIndexToModel(selectedRow);
            String name = (String) model.getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the account for '" + name + "'? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                depositors.remove(modelRow);
                saveDepositors(depositors);
                deleteFromBalance(name);
                deleteFromTransactionLog(name);
                model.removeRow(modelRow);
                JOptionPane.showMessageDialog(this, "Account deleted.");
            }
        } else if (e.getSource() == refreshButton) {
            // Reload data and refresh table
            depositors = loadDepositors();
            String[][] tableData = depositors.toArray(new String[0][]);
            DefaultTableModel newModel = new DefaultTableModel(tableData, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            depositorTable.setModel(newModel);
            sorter.setModel(newModel);
            model = newModel;
        } else if (e.getSource() == monitorButton) {
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an account to monitor.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = depositorTable.convertRowIndexToModel(selectedRow);
            String name = (String) model.getValueAt(modelRow, 0);
            showMonitor(name);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Admin().setVisible(true);
        });
    }
}
