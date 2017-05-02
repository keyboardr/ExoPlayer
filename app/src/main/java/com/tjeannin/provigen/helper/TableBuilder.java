package com.tjeannin.provigen.helper;

import android.database.sqlite.SQLiteDatabase;

import com.tjeannin.provigen.model.Constraint;
import com.tjeannin.provigen.model.Contract;
import com.tjeannin.provigen.model.ContractField;

import java.util.ArrayList;
import java.util.List;

/**
 * Facilitate the creation of an database table from a contract class.
 */
public class TableBuilder {

    private Contract contract;
    private List<Constraint> constraints;

    /**
     * @param contract The contract for which a table will be created.
     */
    public TableBuilder(Contract contract) {
        this.contract = contract;
        constraints = new ArrayList<>();
    }

    /**
     * Adds the specified constraint to the created table.
     *
     * @param columnName               The name of the column on which the constraint is applied.
     * @param constraintType           The type of constraint to apply.
     *                                 One of
     *                                 <ul>
     *                                 <li>{@link Constraint#UNIQUE}</li>
     *                                 <li>{@link Constraint#NOT_NULL}</li>
     *                                 </ul>
     * @param constraintConflictClause The conflict clause to apply in case of constraint violation.
     *                                 One of
     *                                 <ul>
     *                                 <li>{@link Constraint.OnConflict#ABORT}</li>
     *                                 <li>{@link Constraint.OnConflict#FAIL}</li>
     *                                 <li>{@link Constraint.OnConflict#IGNORE}</li>
     *                                 <li>{@link Constraint.OnConflict#REPLACE}</li>
     *                                 <li>{@link Constraint.OnConflict#ROLLBACK}</li>
     *                                 </ul>
     * @return The {@link TableBuilder} instance to allow chaining.
     */
    public TableBuilder addConstraint(String columnName, String constraintType, String constraintConflictClause) {
        constraints.add(new Constraint(columnName, constraintType, constraintConflictClause));
        return this;
    }

    /**
     * @return The SQL expression generated by this {@link TableBuilder}.
     */
    public String getSQL() {

        StringBuilder builder = new StringBuilder("CREATE TABLE ");
        builder.append(contract.getTable()).append(" ( ");

        for (ContractField field : contract.getFields()) {
            builder.append(" ").append(field.name).append(" ").append(field.type);
            if (field.name.equals(contract.getIdField())) {
                builder.append(" PRIMARY KEY AUTOINCREMENT ");
            }
            for (Constraint constraint : constraints) {
                if (constraint.targetColumn.equals(field.name)) {
                    builder.append(" ").append(constraint.type).append(" ON CONFLICT ").append(constraint.conflictClause);
                }
            }
            builder.append(", ");
        }
        builder.deleteCharAt(builder.length() - 2);
        builder.append(" ) ");
        return builder.toString();
    }

    /**
     * Creates the table in the database.
     *
     * @param database The database in which the table need to be created.
     */
    public void createTable(SQLiteDatabase database) {
        database.execSQL(getSQL());
    }
}
