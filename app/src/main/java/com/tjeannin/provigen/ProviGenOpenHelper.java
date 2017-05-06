package com.tjeannin.provigen;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.tjeannin.provigen.helper.TableBuilder;
import com.tjeannin.provigen.helper.TableUpdater;
import com.tjeannin.provigen.model.Contract;

/**
 * A simple implementation of a {@link SQLiteOpenHelper} that:
 * <ul>
 * <li>Create a table for each supplied contract when the database is created for the first time
 * .</li>
 * <li>Add missing table columns when the database version is increased.</li>
 * </ul>
 */
public class ProviGenOpenHelper extends SQLiteOpenHelper {

  private final Class[] contracts;

  /**
   * Create a helper object to create, open, and/or manage a database.
   * This method always returns very quickly.  The database is not actually
   * created or opened until one of {@link #getWritableDatabase} or
   * {@link #getReadableDatabase} is called.
   *
   * @param context      the context to use to open or create the database.
   * @param databaseName the name of the database file, or null for an in-memory database.
   * @param factory      the factory to use for creating cursor objects, or null for the default.
   * @param version      the version of the database. Each time the version is increased,
   *                     missing columns will be added.
   */
  public ProviGenOpenHelper(Context context, @Nullable String databaseName, @Nullable
      CursorFactory factory, int version, Class[] contractClasses) {
    super(context, databaseName, factory, version);
    this.contracts = contractClasses;
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    for (Class contractClass : contracts) {
      new TableBuilder(new Contract(contractClass)).createTable(database);
    }
  }

  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
    if (newVersion > oldVersion) {
      for (Class contractClass : contracts) {
        Contract contract = new Contract(contractClass);
        if (hasTableInDatabase(database, contract)) {
          TableUpdater.addMissingColumns(database, contract);
        } else {
          new TableBuilder(contract).createTable(database);
        }
      }
    }
  }

  public boolean hasTableInDatabase(SQLiteDatabase database,
                                    Contract contract) {

    Cursor cursor = database.rawQuery(
        "SELECT * FROM sqlite_master WHERE name = ? ",
        new String[]{contract.getTable()});
    boolean exists = cursor.getCount() != 0;
    cursor.close();
    return exists;
  }
}
