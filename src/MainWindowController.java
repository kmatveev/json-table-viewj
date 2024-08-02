// import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

public class MainWindowController {

    public static final String GENERATED_JSON_FIELD_KEY = "#";

    public static void main(String[] args) throws Exception {

        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        // UIManager.setLookAndFeel( new FlatLightLaf() );


        MainWindowController controller = new MainWindowController();
    }

    private Integer currentFontSize;

    public MainWindowController() {
        JFrame frame = new JFrame("JSON as table view");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

//        JMenuBar menuBar = new JMenuBar();
//        {
//            JMenu fileMenu = new JMenu("File");
//        }
//        frame.setJMenuBar(menuBar);


        MappedTableModel tableModel = new MappedTableModel();

        JTabbedPane tabbedPane = new JTabbedPane();
        {

            JTextArea jsonText = new JTextArea("");
            JScrollPane jsonTextScrollPane = new JScrollPane(jsonText);
            jsonTextScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
            jsonTextScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

            JComponent jsonTextPanel = new JPanel(new BorderLayout());
            jsonTextPanel.add(jsonTextScrollPane, BorderLayout.CENTER);

            JComponent statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            jsonTextPanel.add(statusBar, BorderLayout.SOUTH);
            JLabel statusText = new JLabel(" ");
            statusBar.add(statusText);

            JComponent commandBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            commandBar.add(new JButton(new AbstractAction("Show as table") {
                @Override
                public void actionPerformed(ActionEvent e) {

                    JsonParser parser = new JsonParser();
                    List<Map<String, JsonElement>> parsed = new ArrayList<>();
                    JsonElement element;
                    try {
                        element = parser.parseString(jsonText.getText());
                    } catch (JsonSyntaxException ex) {
                        String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                        statusText.setText(message);
                        return;
                    }
                    statusText.setText("Valid JSON");
                    if (element instanceof JsonArray) {
                        JsonArray elements = (JsonArray)element;
                        for (JsonElement el : elements) {
                            if (el instanceof JsonObject) {
                                JsonObject jsobj = (JsonObject)el;
                                parsed.add(jsobj.asMap());
                            }
                        }
                    } else if (element instanceof JsonObject) {
                        // if all fields are non-scalar objects, then represent it as keyed table, otherwise as single-row table
                        Map<String, JsonElement> fields = ((JsonObject)element).asMap();
                        boolean hasPrimitiveFields = false;
                        for (JsonElement val : fields.values()) {
                            if (val.isJsonPrimitive() || val.isJsonNull()) {
                                hasPrimitiveFields = true;
                                break;
                            }
                        }
                        if (hasPrimitiveFields) {
                            parsed.add(((JsonObject)element).asMap());
                        } else {
                            for (Map.Entry<String, JsonElement> field : fields.entrySet()) {
                                JsonObject value = (JsonObject) field.getValue();
                                value.addProperty(GENERATED_JSON_FIELD_KEY, field.getKey());
                                parsed.add(value.asMap());
                            }
                        }
                    } else {
                        statusText.setText("Neither JSON Object nor JSON array");
                    }

                    tableModel.clear();
                    tableModel.addItems(parsed);

                    tabbedPane.setSelectedIndex(1);
                }
            }));
            commandBar.add(new JButton(new AbstractAction("Format ") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JsonParser parser = new JsonParser();
                    JsonElement element;
                    try {
                        element = parser.parseString(jsonText.getText());
                        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
                        String formatted = gson.toJson(element);
                        jsonText.setText(formatted);
                        statusText.setText("Valid JSON");
                    } catch (JsonSyntaxException ex) {
                        String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                        statusText.setText(message);
                    }
                }
            }));
            JButton unescapeButton = new JButton(new AbstractAction("Unescape") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jsonText.setText(unescapeQuotes(jsonText.getText()));
                }
            });
            commandBar.add(unescapeButton);
            jsonText.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    unescapeButton.setEnabled(areAllQuotesEscaped(jsonText.getText()));
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    unescapeButton.setEnabled(areAllQuotesEscaped(jsonText.getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    unescapeButton.setEnabled(areAllQuotesEscaped(jsonText.getText()));
                }
            });


            {
                if (currentFontSize == null) {
                    currentFontSize = jsonText.getFont().getSize();
                } else {
                    Font font = jsonText.getFont();
                    float selectedSize = currentFontSize.floatValue();
                    jsonText.setFont(font.deriveFont(selectedSize));
                }
                List<Integer> fontSizes = IntStream.range(5, currentFontSize + 30).mapToObj(Integer::valueOf).collect(Collectors.toList());

                JComboBox<Integer> fontSizeCombo = new JComboBox<>(new DefaultComboBoxModel<>(new Vector<Integer>(fontSizes)));
                fontSizeCombo.setSelectedItem(currentFontSize);
                fontSizeCombo.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        Font font = jsonText.getFont();
                        currentFontSize = (Integer) fontSizeCombo.getSelectedItem();
                        float selectedSize = currentFontSize.floatValue();
                        jsonText.setFont(font.deriveFont(selectedSize));
                    }
                });
                commandBar.add(new JLabel("Font size:"));
                commandBar.add(fontSizeCombo);
            }


            jsonTextPanel.add(commandBar, BorderLayout.NORTH);

            tabbedPane.addTab("JSON text", jsonTextPanel);
        }

        {
            JComponent tableContainer = new JPanel(new BorderLayout());

            JComponent tableStatusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            tableContainer.add(tableStatusBar, BorderLayout.SOUTH);
            JLabel tableStatusText = new JLabel(" ");
            tableStatusBar.add(tableStatusText);


            JTable table = new JTable(tableModel);
            JScrollPane jsonTableScrollPane = new JScrollPane(table);
            jsonTableScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
            jsonTableScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

            tableContainer.add(jsonTableScrollPane, BorderLayout.CENTER);

            tableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    tableStatusText.setText("Rows: " + tableModel.getRowCount());
                }
            });

            tabbedPane.addTab("Table", tableContainer);
        }

        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        frame.setSize(400, 300);
        frame.show();
    }

    public static boolean areAllQuotesEscaped(String str) {
        int qi = 0;
        boolean quotesFound = false;
        boolean quotesEscaped = true;
        while (true) {
            qi = str.indexOf('\"', qi);
            if (qi < 0) break;
            quotesFound = true;
            if (qi == 0 )  {
                quotesEscaped = false;
                break;
            } else if (str.charAt(qi - 1) != '\\') {
                quotesEscaped = false;
                break;
            }
            qi += 1;
            if (qi == str.length()) {
                break;
            }
        }
        return quotesFound && quotesEscaped;
    }

    public static String unescapeQuotes(String str) {
        return str.replaceAll("\\\\\"", "\"");
    }


    private static class MappedTableModel implements TableModel {
        private List<String> keys = new ArrayList<>();
        private List<Map<String, JsonElement>> items = new ArrayList<>();
        private List<TableModelListener> listeners = new ArrayList<>();

        public void clear() {
            keys.clear();
            items.clear();
        }

        public void addItem(Map<String, JsonElement> item) {
            boolean newColumn = false;
            for (String key : item.keySet()) {
                if (!keys.contains(key)) {
                    keys.add(key);
                    newColumn = true;
                }
            }
            this.items.add(item);
            for (TableModelListener l : listeners) {
                l.tableChanged(new TableModelEvent(this, 0, items.size()));
            }
        }

        public void addItems(List<Map<String, JsonElement>> items) {
            boolean newColumn = false;
            for (Map<String, JsonElement> item : items) {
                for (String key : item.keySet()) {
                    if (!keys.contains(key)) {
                        keys.add(key);
                        newColumn = true;
                    }
                }
                this.items.add(item);
            }
            for (TableModelListener l : listeners) {
                l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW, items.size(), TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
            }
        }


        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return keys.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return keys.get(columnIndex);
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
            JsonElement obj = items.get(rowIndex).get(keys.get(columnIndex));
            return obj == null ? "" : String.valueOf(obj);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }
    }

}
