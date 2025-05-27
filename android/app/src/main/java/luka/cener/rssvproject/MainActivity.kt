package luka.cener.rssvproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import luka.cener.rssvproject.ui.theme.RSSVProjectTheme

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import luka.cener.rssvproject.BluetoothService

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothService: BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bluetoothService = BluetoothService()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                    1
                )
            }
        }

        setContent {
            RSSVProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RgbLedControlApp(
                        modifier = Modifier.padding(innerPadding),
                        bluetoothService = bluetoothService
                    )
                }
            }
        }
    }

    //Zatvaranje konekcije kad se izade iz aplikacije
    override fun onStop() {
        super.onStop()
        bluetoothService.closeConnection()
    }
}

@Composable
fun RgbLedControlApp(modifier: Modifier, bluetoothService: BluetoothService) {
    var red by remember { mutableIntStateOf(0) }
    var green by remember { mutableIntStateOf(0) }
    var blue by remember { mutableIntStateOf(0) }
    var isOn by remember { mutableStateOf(false) }

    var lastSentTime by remember { mutableStateOf(0L) }

    var showDeviceDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var selectedDeviceName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val pairedDevices = if (ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        bluetoothAdapter?.bondedDevices ?: emptySet()
    } else emptySet()

    val deviceList = pairedDevices.toList()

    fun sendColorData() {
        if (selectedDevice != null) {
            val data = "$red,$green,$blue,${if (isOn) 1 else 0}"
            bluetoothService.sendData(data)
        }
    }

    //Kako ne bi doslo do zagusenja, info se salje svakih 50ms
    fun sendColorDataThrottle() {
        val now = System.currentTimeMillis()
        if(now - lastSentTime > 50) {
            lastSentTime = now
            sendColorData()
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RGB LED Control App",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
        )

        //Boje
        Text("Colors", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
        ColorSlider("Red", red, Color.Red) {
            red = it.coerceIn(0, 255)
            if (selectedDevice != null)
                sendColorDataThrottle()
        }
        Spacer(modifier = Modifier.height(4.dp))
        ColorSlider("Green", green, Color.Green) {
            green = it.coerceIn(0, 255)
            if (selectedDevice != null)
                sendColorDataThrottle()
        }
        Spacer(modifier = Modifier.height(4.dp))
        ColorSlider("Blue", blue, Color.Blue) {
            blue = it.coerceIn(0, 255)
            if (selectedDevice != null)
                sendColorDataThrottle()
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Power
        Text("Power", fontSize = 16.sp, modifier = Modifier.padding(bottom = 2.dp))
        PowerSwitch(isOn = isOn, onToggle = {
            isOn = it
            selectedDevice?.let {
                sendColorDataThrottle()
            }
        })

        Spacer(modifier = Modifier.height(16.dp))

        //Connect device
        Button(
            onClick = { showDeviceDialog = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text("Connect with device")
        }

        selectedDeviceName?.let {
            Text(
                text = "Selected device: $it",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (showDeviceDialog) {
            AlertDialog(
                onDismissRequest = { showDeviceDialog = false },
                confirmButton = {},
                title = {
                    Text("Select a device")
                },
                text = {
                    Column {
                        deviceList.forEach { device ->
                            TextButton(
                                onClick = {
                                    if (bluetoothService.connectToDevice(device)) {
                                        selectedDevice = device
                                        selectedDeviceName = device.name
                                    }
                                    showDeviceDialog = false
                                }
                            ) {
                                Text(device.name ?: device.address)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Int, color: Color, onValueChange: (Int) -> Unit) {
    Column {
        Text(text = label, fontSize = 14.sp, modifier = Modifier.padding(start = 20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                modifier = Modifier.padding(start = 4.dp),
                onClick = { onValueChange(value - 1) }) {
                Text("-", fontSize = 16.sp)
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                modifier = Modifier.padding(end = 4.dp),
                onClick = { onValueChange(value + 1) }) {
                Text("+", fontSize = 16.sp)
            }

            Text(
                text = value.toString(),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 4.dp, end = 2.dp).width(32.dp)
            )
        }
    }
}

@Composable
fun PowerSwitch(isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Switch(
            checked = isOn,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Green,
                uncheckedThumbColor = Color.Red
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isOn) "ON" else "OFF",
            fontSize = 14.sp,
            color = if (isOn) Color.Green else Color.Red,
            fontWeight = FontWeight.SemiBold
        )
    }
}