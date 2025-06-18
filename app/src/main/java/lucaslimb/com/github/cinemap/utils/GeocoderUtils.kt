package lucaslimb.com.github.cinemap.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

suspend fun getLatLngFromCountryName(context: Context, countryName: String): LatLng? {
    return withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocationName(countryName, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                return@withContext LatLng(address.latitude, address.longitude)
            }
        } catch (e: IOException) {
            Log.e("GeocodingUtils", "Não foi possível obter endereço para $countryName: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("GeocodingUtils", "Nome de país inválido: $countryName - ${e.message}")
        }
        return@withContext null
    }
}