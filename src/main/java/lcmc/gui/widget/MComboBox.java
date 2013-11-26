/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package lcmc.gui.widget;

import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/**
 *
 * @author rasto
 */
public final class MComboBox<E> extends JComboBox<E> {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    private boolean layingOut = false;
    
    public MComboBox(final E[] items) {
        super(items);
    }

    public MComboBox(@SuppressWarnings("UseOfObsoleteCollectionType") final java.util.Vector<E> items) {
        super(items);
    }

    public MComboBox(final javax.swing.ComboBoxModel<E> aModel) {
        super(aModel);
    }

    @Override
    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }

    /** Get new size if popup items are wider than the item. */
    @Override
    public Dimension getSize() {
        final Dimension dim = super.getSize();
        if (!layingOut) {
            final Object c = getUI().getAccessibleChild(this, 0);
            if (c instanceof JPopupMenu) {
                final JScrollPane scrollPane = (JScrollPane) ((JPopupMenu) c).getComponent(0);
                final Dimension size = scrollPane.getPreferredSize();
                final JComponent view = (JComponent) scrollPane.getViewport().getView();
                final int newSize = view.getPreferredSize().width + 2;
                dim.width = Math.max(dim.width, newSize);
            }
        }
        return dim;
    }
    
}
