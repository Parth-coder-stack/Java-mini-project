package ui;

import service.ConnectionService;
import ui.components.FlatButton;
import ui.components.FormBuilder;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Edit host / port / user / password for MySQL and test the connection. */
public class DBSettingsDialog extends JDialog {

    private final ConnectionService service = new ConnectionService();
    private final JTextField host = UIStyles.textField(18);
    private final JTextField port = UIStyles.textField(6);
    private final JTextField dbName = UIStyles.textField(12);
    private final JTextField user = UIStyles.textField(12);
    private final JPasswordField password = UIStyles.passwordField(12);
    private final JLabel result = UIStyles.label(" ", UIStyles.BOLD, UIStyles.MUTED);
    private boolean saved;

    public DBSettingsDialog(JFrame owner) {
        super(owner, "Database settings", true);
        host.setText(service.getHost());
        port.setText(service.getPort());
        dbName.setText(service.getDbName());
        user.setText(service.getUser());
        password.setText(service.getPassword());

        JLabel help = UIStyles.muted("<html>MySQL on this computer: use <b>localhost</b>.<br>"
                + "MySQL on another laptop: use that laptop's IPv4 address (run <b>ipconfig</b> there),<br>"
                + "and make sure both are on the same Wi-Fi. See SETUP_README.txt.</html>");

        JPanel form = new FormBuilder()
                .row(help)
                .row(FormBuilder.labeled("Host / IP address", host), FormBuilder.labeled("Port", port))
                .row(FormBuilder.labeled("Database", dbName), FormBuilder.labeled("User", user))
                .field("MySQL password", password)
                .row(result)
                .build();

        FlatButton save = UIStyles.primary("Save and test");
        FlatButton close = UIStyles.secondary("Close");
        save.addActionListener(e -> saveAndTest());
        close.addActionListener(e -> dispose());
        JPanel buttons = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(close);
        buttons.add(save);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(UIStyles.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    private void saveAndTest() {
        result.setForeground(UIStyles.MUTED);
        result.setText("Testing...");
        String pw = new String(password.getPassword());
        Async.run(this, () -> {
            service.save(host.getText(), port.getText(), dbName.getText(), user.getText(), pw);
            return service.test();
        }, error -> {
            saved = true;
            if (error == null) {
                result.setForeground(UIStyles.SUCCESS);
                result.setText("Connected! Settings saved to db.properties.");
            } else {
                result.setForeground(UIStyles.DANGER);
                result.setText("<html>Saved, but could not connect:<br>"
                        + error.replace("\n", "<br>") + "</html>");
                pack();
            }
        });
    }

    /** Shows the dialog; returns true if settings were saved. */
    public boolean showDialog() {
        setVisible(true);
        return saved;
    }
}
