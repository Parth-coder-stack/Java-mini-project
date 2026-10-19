package ui;

import ui.components.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/** Colours, fonts and factory methods so every screen looks the same. */
public final class UIStyles {

    public static final Color PRIMARY = new Color(79, 70, 229);
    public static final Color PRIMARY_HOVER = new Color(67, 56, 202);
    public static final Color PRIMARY_LIGHT = new Color(238, 242, 255);
    public static final Color SIDEBAR = new Color(17, 24, 39);
    public static final Color SIDEBAR_HOVER = new Color(31, 41, 55);
    public static final Color SIDEBAR_TEXT = new Color(209, 213, 219);
    public static final Color BG = new Color(243, 244, 246);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(229, 231, 235);
    public static final Color TEXT = new Color(17, 24, 39);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color SUCCESS_LIGHT = new Color(220, 252, 231);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color DANGER_LIGHT = new Color(254, 226, 226);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color WARNING_LIGHT = new Color(254, 243, 199);

    public static final String FONT_NAME = pickFont("Segoe UI", "Inter", "Helvetica Neue", "Arial");
    public static final Font BODY = font(Font.PLAIN, 14);
    public static final Font BOLD = font(Font.BOLD, 14);
    public static final Font SMALL = font(Font.PLAIN, 12);
    public static final Font H1 = font(Font.BOLD, 26);
    public static final Font H2 = font(Font.BOLD, 19);
    public static final Font H3 = font(Font.BOLD, 15);

    /** Distinct colours for course blocks in the timetable. */
    public static final List<Color> PALETTE = Arrays.asList(
            new Color(99, 102, 241), new Color(16, 185, 129), new Color(245, 158, 11),
            new Color(239, 68, 68), new Color(14, 165, 233), new Color(168, 85, 247),
            new Color(236, 72, 153), new Color(34, 197, 94));

    private UIStyles() {
    }

    private static String pickFont(String... names) {
        List<String> available = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String n : names) {
            if (available.contains(n)) {
                return n;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FONT_NAME, style, Math.round(size));
    }

    /** Nimbus look and feel tinted with the app colours. Call once before creating windows. */
    public static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Nimbus not available, using default look and feel.");
        }
        UIManager.put("defaultFont", BODY);
        UIManager.put("control", BG);
        UIManager.put("nimbusBase", new Color(55, 65, 81));
        UIManager.put("nimbusBlueGrey", new Color(209, 213, 219));
        UIManager.put("nimbusFocus", new Color(165, 180, 252));
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusSelection", PRIMARY);
        UIManager.put("textForeground", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(249, 250, 251));
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox:\"ComboBox.listRenderer\".background", Color.WHITE);
        UIManager.put("Table.font", BODY);
        UIManager.put("TableHeader.font", BOLD);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BOLD);
    }

    // ------------------------------------------------------------------ factories

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel title(String text)    { return label(text, H2, TEXT); }
    public static JLabel heading(String text)  { return label(text, H3, TEXT); }
    public static JLabel muted(String text)    { return label(text, SMALL, MUTED); }

    public static FlatButton primary(String text)   { return new FlatButton(text, PRIMARY, PRIMARY_HOVER, Color.WHITE); }
    public static FlatButton danger(String text)    { return new FlatButton(text, DANGER, new Color(185, 28, 28), Color.WHITE); }
    public static FlatButton success(String text)   { return new FlatButton(text, SUCCESS, new Color(21, 128, 61), Color.WHITE); }
    public static FlatButton secondary(String text) { return new FlatButton(text, Color.WHITE, BG, TEXT, BORDER); }

    /** Text-only button that looks like a hyperlink. */
    public static FlatButton link(String text) {
        FlatButton b = new FlatButton(text, null, null, PRIMARY);
        b.setFont(BOLD);
        b.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return b;
    }

    private static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    public static JTextField textField(int columns) {
        JTextField f = new JTextField(columns);
        f.setUI(new BasicTextFieldUI());
        styleField(f);
        return f;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField f = new JPasswordField(columns);
        f.setUI(new BasicPasswordFieldUI());
        styleField(f);
        return f;
    }

    private static void styleField(JTextField f) {
        f.setFont(BODY);
        f.setBackground(Color.WHITE);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(fieldBorder());
    }

    public static JTextArea textArea(int rows, int cols) {
        JTextArea a = new JTextArea(rows, cols);
        a.setFont(BODY);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    /** White rounded "card" with padding. */
    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return p;
    }

    /** Card with a heading at the top and the given content below. */
    public static JPanel card(String heading, JComponent content) {
        JPanel p = card();
        p.add(heading(heading), BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Transparent panel, handy for layouts. */
    public static JPanel clear(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }
}
