package sim;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;

/**
 * @author Roman Elizarov
 */
public class MessagesTableModel implements TableModel {
    private static final String[] COLS = { "From", "To", "Description" };

    private final NetworkUIModel model;
    private final ArrayList<TableModelListener> listeners = new ArrayList<>();

    public MessagesTableModel(NetworkUIModel model) {
        this.model = model;
        model.addMsgsUpdateListener(this::fireListeners);
    }

    @Override
    public int getRowCount() {
        return model.getMsgs().size();
    }

    @Override
    public int getColumnCount() {
        return COLS.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLS[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        AbstractMsg msg = model.getMsgs().get(rowIndex);
        switch (columnIndex) {
            case 0: return msg.from.toString();
            case 1: return msg.to.toString();
            case 2: return msg.getDescription();
            default: throw new AssertionError();
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // not supported
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    private void fireListeners() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener l : listeners) {
            l.tableChanged(e);
        }
    }
}
