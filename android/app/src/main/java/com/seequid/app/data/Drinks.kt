package com.seequid.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "drinks")
data class Drink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val amountMl: Int,
)

@Dao
interface DrinkDao {
    @Insert
    suspend fun insert(drink: Drink)

    @Query("SELECT * FROM drinks WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun observeSince(since: Long): Flow<List<Drink>>

    @Query("DELETE FROM drinks WHERE id = (SELECT id FROM drinks ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLatest()
}

@Database(entities = [Drink::class], version = 1, exportSchema = false)
abstract class SeequidDatabase : RoomDatabase() {
    abstract fun drinks(): DrinkDao

    companion object {
        fun create(context: Context): SeequidDatabase =
            Room.databaseBuilder(context, SeequidDatabase::class.java, "seequid.db").build()
    }
}
