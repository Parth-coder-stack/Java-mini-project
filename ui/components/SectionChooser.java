package ui.components;

import model.Section;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/** Drop-down of sections with a short, readable label. */
public class SectionChooser extends JComboBox<Section> {

    public SectionChooser() {
        setPreferredSize(new Dimension(420, 34));
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                String text = value instanceof Section ? label((Section) value) : "No sections";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
    }

    public static String label(Section s) {
        return s.getCourseCode() + " " + s.getComponentType() + " " + s.getSectionNumber()
                + "   |   " + s.getSlot() + "   |   " + s.getEnrolledCount() + " students";
    }

    /** Replaces the items, keeping the same section selected if it is still there. */
    public void setSections(List<Section> sections) {
        Section old = getSelectedSection();
        removeAllItems();
        Section select = null;
        for (Section s : sections) {
            addItem(s);
            if (old != null && old.getSectionId() == s.getSectionId()) {
                select = s;
            } else if (old == null && select == null && s.getEnrolledCount() > 0) {
                select = s;   // first time: open on a section that actually has students
            }
        }
        if (select != null) {
            setSelectedItem(select);
        }
    }

    public Section getSelectedSection() {
        return (Section) getSelectedItem();
    }
}
