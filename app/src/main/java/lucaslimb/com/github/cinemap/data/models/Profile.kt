package lucaslimb.com.github.cinemap.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class Profile (

    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,
    val title: String,
    val filmsCount: Int,
    val countriesCount: Int,
    val continentsCount: Int,
    val yearsCount: Int
)