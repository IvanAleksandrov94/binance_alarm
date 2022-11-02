package com.grapesapps.cryptochecker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.grapesapps.cryptochecker.models.PriceModel
import com.grapesapps.cryptochecker.services.CryptoAlarmService
import com.grapesapps.cryptochecker.ui.theme.CryptoCheckerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import kotlin.random.Random.Default.nextBoolean


class MainActivity : ComponentActivity() {

    private fun isServiceRunning(serviceClass: Class<out Service>) =
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            ?.map { it.service.className }
            ?.contains(serviceClass.name) ?: false


    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val sharedPref = SharedPrefManager(applicationContext)
        val isRunning = this.isServiceRunning(CryptoAlarmService::class.java)

        setContent {
            val initialCryptoCurrency: String = sharedPref.getCryptoCurrency() ?: "TWT"
            val focusManager = LocalFocusManager.current
            var isRunningService by remember { mutableStateOf(isRunning) }
            var cryptoCurrency by rememberSaveable { mutableStateOf(initialCryptoCurrency) }

            var minPrice: String? by rememberSaveable { mutableStateOf("0.5") }
            var maxPrice: String? by rememberSaveable { mutableStateOf("3.0") }


            fun getPrice(cryptoCurrency: String, pair: String = "USDT"): PriceModel? {
                val url: String = baseUrlV3 + "ticker/price?symbol=$cryptoCurrency$pair"
                val requestBinance = Request.Builder().url(url).build()

                OkHttpClient().newCall(requestBinance).execute().use { response ->
                    if (response.isSuccessful) {
                        val gson = Gson()
                        try {
                            return@getPrice gson.fromJson(response.body?.string(), PriceModel::class.java)
                        } catch (_: Exception) {
                            return null
                        }
                    }
                }
                return null
            }

            fun startService() {
                try {
                    if (cryptoCurrency.isEmpty()) {
                        throw Exception("Криптовалютная пара не может быть пустой")
                    }
                    if (isRunningService) {
                        isRunningService = false
                        val intent = Intent(applicationContext, CryptoAlarmService::class.java).apply {
                            action = "STOP_ACTION"
                        }
                        // стопает сервис с акшеном "STOP_ACTION" для отключения doze
                        applicationContext.startService(intent)
                        cryptoCurrency = ""
                        throw Exception("foreground service убит")
                    }
                    focusManager.clearFocus()

                    val cryptoCurrencyUpper = cryptoCurrency.trim().uppercase(Locale.getDefault())
                    val priceModel = getPrice(cryptoCurrencyUpper) ?: throw Exception("Ошибка запроса криптовалюты")
                    val formattedPrice = String.format("%.3f", priceModel.price.toDoubleOrNull())

                    val intent = Intent(applicationContext, CryptoAlarmService::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        action = "START_ACTION"
                        putExtra("TITLE_CURRENCY", cryptoCurrencyUpper)
                        putExtra("TITLE_PRICE", "\$$formattedPrice")
                        putExtra("MIN_PRICE", minPrice)
                        putExtra("MIN_PRICE", maxPrice)
                    }

                    sharedPref.saveCryptoCurrency(cryptoCurrencyUpper)
                    applicationContext.startForegroundService(intent)
                    isRunningService = true

                } catch (e: Exception) {
                    sharedPref.removeCryptoCurrency()
                    isRunningService = false
                    runOnUiThread {
                        Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            CryptoCheckerTheme {
                Scaffold(
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() } // This is mandatory
                        ) {
                            focusManager.clearFocus()
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box() {
                            Text(
                                text = "Сервис запущен",
                                style = TextStyle(
                                    color = if (isRunningService) Green else Transparent,
                                    fontSize = 30.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            value = minPrice ?: "",
                            label = { Text("Если цена меньше чем", style = TextStyle(color = Gray)) },
                            placeholder = {
                                Text(
                                    if (nextBoolean()) "4.345" else {
                                        "1.3343"
                                    }, style = TextStyle(color = Gray)
                                )
                            },
                            singleLine = true,
                            enabled = !isRunningService,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Number
                            ),
                            onValueChange = { newText -> minPrice = newText }
                        )
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            value = maxPrice ?: "",
                            label = { Text("Если цена больше чем", style = TextStyle(color = Gray)) },
                            placeholder = {
                                Text(
                                    if (nextBoolean()) "10.775" else {
                                        "3.9843"
                                    }, style = TextStyle(color = Gray)
                                )
                            },
                            singleLine = true,
                            enabled = !isRunningService,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Number
                            ),
                            onValueChange = { newText -> maxPrice = newText }
                        )
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            value = cryptoCurrency,
                            label = { Text("Введите криптовалюту", style = TextStyle(color = Gray)) },
                            placeholder = {
                                Text(
                                    if (nextBoolean()) "BTC" else {
                                        "ETH"
                                    }, style = TextStyle(color = Gray)
                                )
                            },
                            singleLine = true,
                            enabled = !isRunningService,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        startService()
                                    }
                                }
                            ),
                            onValueChange = { newText -> cryptoCurrency = newText }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 30.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        startService()
                                    }
                                },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    verticalArrangement = Arrangement.SpaceAround,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isRunningService) "Стоп" else "Старт",
                                        style = TextStyle(color = Gray)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

