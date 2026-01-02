package com.alfadjri28.e_witank.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken



data class ControllerData(
    var model: String?,
    var controllerIP: String,
    var controllerID: String? = "saveFile",
    var camIP: String?,
    var camID: String
)

class LocalStorageControllerRC(context: Context) {
    private val PREF_NAME = "controller_rc_storage"
    private val KEY_CONTROLLERS = "controllers"
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveController(data: ControllerData) {
        val currentList = getController().toMutableList()
        val index = currentList.indexOfFirst { it.controllerIP == data.controllerIP }
        if (index >= 0) currentList[index] = data else currentList.add(data)
        sharedPreferences.edit().putString(KEY_CONTROLLERS, gson.toJson(currentList)).apply()
    }

    fun updateController(camID: String, updatedData: ControllerData) {
        val currentList = getController().toMutableList()
        val index = currentList.indexOfFirst { it.camID == camID }
        if (index != -1) {
            currentList[index] = updatedData
            saveControllerList(currentList)
        }
    }

    fun getCamIPByCamID(camID: String): String? {
        val currentList = getController()
        return currentList.firstOrNull { it.camID == camID }?.camIP
    }


    fun getController(): List<ControllerData> {
        val json = sharedPreferences.getString(KEY_CONTROLLERS, null) ?: return emptyList()
        val type = object : TypeToken<List<ControllerData>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        android.util.Log.d("LocalStorageControllerRC", "🧹 Semua data dihapus dari SharedPreferences")
    }

    private fun saveControllerList(list: List<ControllerData>) {
        sharedPreferences.edit().putString(KEY_CONTROLLERS, gson.toJson(list)).apply()
    }

    fun deleteControllerByIP(ip: String) {
        val currentList = getController().toMutableList()
        val newList = currentList.filterNot { it.controllerIP == ip }
        saveControllerList(newList)
    }

}
