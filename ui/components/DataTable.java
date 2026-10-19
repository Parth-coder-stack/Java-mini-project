package ui.components;

import ui.UIStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A styled, read-only, sortable JTable bound to a list of objects.
 * Optionally shows a live filter box above the table.
 *
 * @param <T> type of object shown in each row
 */
public class DataTable<T> extends JPanel {

    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final Function<T, Object[]> rowMapper;
    private final List<T> items = new ArrayList<>();
    private JTextField filterField;

    public DataTable(String[] columns, Function<T, Object[]> rowMapper) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        this.rowMapper = rowMapper;
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        style(table);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        add(UIStyles.scroll(table), BorderLayout.CENTER);
    }

    /** Applies the app's table look to any JTable. */
    public static void style(JTable table) {
        table.setRowHeight(32);
        table.setFont(UIStyles.BODY);
        table.setShowVerticalLines(false);
        table.setGridColor(UIStyles.BORDER);
        table.setSelectionBackground(UIStyles.PRIMARY_LIGHT);
        table.setSelectionForeground(UIStyles.TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                           int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
                l.setFont(UIStyles.BOLD);
                l.setForeground(UIStyles.MUTED);
                l.setBackground(new Color(249, 250, 251));
                l.setOpaque(true);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyles.BORDER),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                return l;
            }
        });
    }

    /** Adds a "type to filter" box above the table. */
    public DataTable<T> withFilter(String placeholder) {
        filterField = UIStyles.textField(24);
        filterField.setToolTipText(placeholder);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        JPanel top = UIStyles.clear(new BorderLayout(8, 0));
        top.add(UIStyles.muted(placeholder), BorderLayout.WEST);
        top.add(filterField, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        return this;
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }

    public void setItems(List<T> newItems) {
        items.clear();
        items.addAll(newItems);
        model.setRowCount(0);
        for (T item : items) {
            model.addRow(rowMapper.apply(item));
        }
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** The object in the selected row, or null. */
    public T getSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < items.size() ? items.get(modelRow) : null;
    }

    public void onSelect(Consumer<T> listener) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                listener.accept(getSelected());
            }
        });
    }

    public void onDoubleClick(Consumer<T> listener) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getSelected() != null) {
                    listener.accept(getSelected());
                }
            }
        });
    }

    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public JTable getTable() {
        return table;
    }

    /** Colours well-known status words (Eligible, Absent, Rejected, ...) and percentages. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                       int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, false, row, col);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            l.setFont(UIStyles.BODY);
            l.setForeground(UIStyles.TEXT);
            String text = value == null ? "" : value.toString();
            l.setToolTipText(text.length() > 25 ? text : null);   // full text on hover when truncated
            if (text.startsWith("Eligible") || text.equals("Present") || text.equals("Approved")
                    || text.startsWith("Enrolled") || text.equals("Completed") || text.equals("Verified")
                    || text.equals("Ready") || text.equals("Watched")) {
                l.setForeground(UIStyles.SUCCESS);
                l.setFont(UIStyles.BOLD);
            } else if (text.startsWith("Blocked") || text.equals("Absent") || text.equals("Rejected")
                    || text.equals("Dropped") || text.equals("Full") || text.endsWith("(low)")
                    || text.startsWith("Needs ") || text.equals("On probation") || text.equals("New")) {
                l.setForeground(UIStyles.DANGER);
                l.setFont(UIStyles.BOLD);
            } else if (text.equals("Late") || text.equals("Submitted") || text.equals("Waiting")
                    || text.equals("Not submitted") || text.equals("Not verified") || text.equals("Not watched")) {
                l.setForeground(UIStyles.WARNING);
                l.setFont(UIStyles.BOLD);
            }
            return l;
        }
    }
}
