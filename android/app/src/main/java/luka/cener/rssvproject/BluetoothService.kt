package luka.cener.rssvproject

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothService {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice): Boolean {
        return try {
            val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun sendData(data: String) {
        try {
            outputStream?.write((data + "\n").toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun closeConnection() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
