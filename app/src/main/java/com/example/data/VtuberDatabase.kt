package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vtuber_characters")
data class VtuberCharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tagline: String,
    val imageUri: String?, // Custom imported photo URI from Android Photo Picker or internal file
    val archetypeId: Int = 0, // 0 = Celestia (Cyber Idol), 1 = Kuro (Neko Hacker), 2 = Aether (Starlight Mage), 3 = Custom Photo Rig
    val hairColorHex: Long = 0xFF00E5FF,
    val eyeColorHex: Long = 0xFFFF2E93,
    val outfitColorHex: Long = 0xFF7C4DFF,
    // Normalized Live2D Mesh Rigging Anchor Points (0.0f .. 1.0f on Character Canvas)
    val leftEyeX: Float = 0.37f,
    val leftEyeY: Float = 0.43f,
    val rightEyeX: Float = 0.63f,
    val rightEyeY: Float = 0.43f,
    val mouthX: Float = 0.50f,
    val mouthY: Float = 0.58f,
    val headPivotX: Float = 0.50f,
    val headPivotY: Float = 0.68f,
    // Live2D Physics & Deformation Parameters
    val physicsSpring: Float = 0.72f,
    val hairSwayIntensity: Float = 0.80f,
    val breathingSpeed: Float = 1.0f,
    val eyeBlinkSensitivity: Float = 0.85f,
    val mouthLipSyncGain: Float = 1.15f,
    val headTiltSensitivity: Float = 1.0f,
    val accessoryId: Int = 1, // 0=None, 1=Cyber Cat Ears, 2=Starlight Halo, 3=Holo Visor, 4=Devil Horns
    val bgPresetId: Int = 0, // 0=Cyber Grid, 1=Green Screen Chroma, 2=Blue Screen Chroma, 3=Nebula Stage, 4=Sakura Night
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface VtuberCharacterDao {
    @Query("SELECT * FROM vtuber_characters ORDER BY updatedAt DESC")
    fun observeAllCharacters(): Flow<List<VtuberCharacterEntity>>

    @Query("SELECT COUNT(*) FROM vtuber_characters")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: VtuberCharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: VtuberCharacterEntity)

    @Query("DELETE FROM vtuber_characters WHERE id = :id")
    suspend fun deleteCharacter(id: Long)
}

@Database(entities = [VtuberCharacterEntity::class], version = 1, exportSchema = false)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun characterDao(): VtuberCharacterDao
}

class VtuberRepository(private val dao: VtuberCharacterDao) {
    val characters: Flow<List<VtuberCharacterEntity>> = dao.observeAllCharacters()

    suspend fun ensureDefaultPresets() {
        if (dao.getCount() == 0) {
            val presets = listOf(
                VtuberCharacterEntity(
                    name = "Celestia Aria",
                    tagline = "Cyber-Idol Starlight • Rigging Live2D Penuh",
                    imageUri = null,
                    archetypeId = 0,
                    hairColorHex = 0xFF90E0FF,
                    eyeColorHex = 0xFFFF2E93,
                    outfitColorHex = 0xFF7C4DFF,
                    physicsSpring = 0.78f,
                    hairSwayIntensity = 0.85f,
                    breathingSpeed = 1.0f,
                    accessoryId = 2,
                    bgPresetId = 0
                ),
                VtuberCharacterEntity(
                    name = "Kuro Neko-X",
                    tagline = "Techwear Phantom • Respons Ekspresi Cepat",
                    imageUri = null,
                    archetypeId = 1,
                    hairColorHex = 0xFF282C44,
                    eyeColorHex = 0xFFFFD54F,
                    outfitColorHex = 0xFF00E5FF,
                    physicsSpring = 0.65f,
                    hairSwayIntensity = 0.70f,
                    breathingSpeed = 1.15f,
                    accessoryId = 1,
                    bgPresetId = 3
                ),
                VtuberCharacterEntity(
                    name = "Luna Sakura",
                    tagline = "Mystic Kitsune • Ayunan Rambut Lembut",
                    imageUri = null,
                    archetypeId = 2,
                    hairColorHex = 0xFFFF80AB,
                    eyeColorHex = 0xFF00E676,
                    outfitColorHex = 0xFF536DFE,
                    physicsSpring = 0.90f,
                    hairSwayIntensity = 0.95f,
                    breathingSpeed = 0.85f,
                    accessoryId = 3,
                    bgPresetId = 4
                )
            )
            presets.forEach { dao.insertCharacter(it) }
        }
    }

    suspend fun saveCharacter(character: VtuberCharacterEntity): Long {
        return dao.insertCharacter(character.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateCharacter(character: VtuberCharacterEntity) {
        dao.updateCharacter(character.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCharacter(id: Long) {
        dao.deleteCharacter(id)
    }
}
