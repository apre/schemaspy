package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;

/**
 * Format table data into .dot format to feed to Graphvis' dot program.
 *
 * @author John Currier
 */
public class DotConnectorFinder {
    private static DotConnectorFinder instance = new DotConnectorFinder();

    /**
     * Singleton - prevent creation
     */
    private DotConnectorFinder() {
    }

    public static DotConnectorFinder getInstance() {
        return instance;
    }

    /**
     *
     * @param table Table
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as {@link DotConnector}s)
     */
    public Set<DotConnector> getRelatedConnectors(Table table, boolean includeImplied) {
        Set<DotConnector> relationships = new HashSet<DotConnector>();

        for (TableColumn column : table.getColumns()) {
            relationships.addAll(getRelatedConnectors(column, null, false, includeImplied));
        }

        return relationships;
    }

    /**
     * Get all the relationships that exist between these two tables.
     *
     * @param table1 Table
     * @param table2 Table
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as {@link DotConnector}s)
     */
    public Set<DotConnector> getRelatedConnectors(Table table1, Table table2, boolean includeExcluded, boolean includeImplied) {
        Set<DotConnector> relationships = new HashSet<DotConnector>();

        for (TableColumn column : table1.getColumns()) {
            relationships.addAll(getRelatedConnectors(column, table2, includeExcluded, includeImplied));
        }

        for (TableColumn column : table2.getColumns()) {
            relationships.addAll(getRelatedConnectors(column, table1, includeExcluded, includeImplied));
        }

        return relationships;
    }

    /**
     * @param column TableColumn
     * @param targetTable Table
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as {@link DotConnector}s)
     */
    private Set<DotConnector> getRelatedConnectors(TableColumn column, Table targetTable, boolean includeExcluded, boolean includeImplied) {
        Set<DotConnector> relatedConnectors = new HashSet<DotConnector>();
        if (!includeExcluded && column.isExcluded())
            return relatedConnectors;

        for (TableColumn parentColumn : column.getParents()) {
            Table parentTable = parentColumn.getTable();
            if (targetTable != null && parentTable != targetTable)
                continue;
            if (targetTable == null && !includeExcluded && parentColumn.isExcluded())
                continue;
            boolean implied = column.getParentConstraint(parentColumn).isImplied();
            if (!implied || includeImplied) {
                relatedConnectors.add(new DotConnector(parentColumn, column, implied));
            }
        }

        for (TableColumn childColumn : column.getChildren()) {
            Table childTable = childColumn.getTable();
            if (targetTable != null && childTable != targetTable)
                continue;
            if (targetTable == null && !includeExcluded && childColumn.isExcluded())
                continue;
            boolean implied = column.getChildConstraint(childColumn).isImplied();
            if (!implied || includeImplied) {
                relatedConnectors.add(new DotConnector(column, childColumn, implied));
            }
        }

        return relatedConnectors;
    }
}
