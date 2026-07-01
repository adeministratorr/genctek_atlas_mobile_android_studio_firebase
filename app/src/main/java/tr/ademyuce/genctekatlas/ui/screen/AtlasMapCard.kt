package tr.ademyuce.genctekatlas.ui.screen

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import tr.ademyuce.genctekatlas.data.model.Event
import tr.ademyuce.genctekatlas.data.model.Project

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AtlasMapCard(
    events: List<Event>,
    projects: List<Project>,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var mapReady by remember { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    val cityStats = remember(events, projects) {
        buildCityStats(events, projects)
    }
    val mapStateJson = remember(cityStats, selectedCity) {
        buildMapStateJson(cityStats, selectedCity)
    }

    MobileCard(modifier = modifier) {
        Text(
            text = "Harita",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(228.dp)
        ) {
            AndroidView(
                factory = { viewContext ->
                    val mainHandler = Handler(Looper.getMainLooper())
                    WebView(viewContext).apply {
                        webView = this
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                view.evaluateJavascript("window.setAtlasMapState($mapStateJson);", null)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request.isForMainFrame) {
                                    mainHandler.post {
                                        mapFailed = true
                                        mapReady = false
                                    }
                                }
                            }
                        }
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            allowFileAccess = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMapReady() {
                                mainHandler.post {
                                    mapReady = true
                                    mapFailed = false
                                    evaluateJavascript("window.setAtlasMapState($mapStateJson);", null)
                                }
                            }

                            @JavascriptInterface
                            fun onCitySelected(cityName: String) {
                                mainHandler.post {
                                    onCitySelected(normalizeMapCity(cityName))
                                }
                            }

                            @JavascriptInterface
                            fun onMapError(message: String) {
                                mainHandler.post {
                                    mapFailed = true
                                    mapReady = false
                                }
                            }
                        }, "AndroidInterface")
                        loadDataWithBaseURL(
                            "file:///android_asset/",
                            loadAtlasMapHtml(viewContext),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { view ->
                    webView = view
                    if (mapReady && !mapFailed) {
                        view.evaluateJavascript("window.setAtlasMapState($mapStateJson);", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (mapFailed) {
                CityFallbackSelector(
                    cityStats = cityStats,
                    selectedCity = selectedCity,
                    onCitySelected = onCitySelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (!mapReady && !mapFailed) {
            Text(
                text = "Harita yükleniyor. Yükleme başarısız olursa şehir seçici otomatik açılır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CityFallbackSelector(
    cityStats: List<MapCityStat>,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cityStats.take(18).forEach { stat ->
            FilterChip(
                selected = selectedCity.equals(stat.city, ignoreCase = true),
                onClick = {
                    onCitySelected(if (selectedCity.equals(stat.city, ignoreCase = true)) "" else stat.city)
                },
                label = { Text("${stat.city} ${stat.eventsCount + stat.projectsCount}") },
                leadingIcon = if (selectedCity.equals(stat.city, ignoreCase = true)) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                } else {
                    { Icon(Icons.Default.LocationOn, contentDescription = null) }
                },
                shape = MobileCardShape
            )
        }
    }
}

private data class MapCityStat(
    val city: String,
    val eventsCount: Int,
    val projectsCount: Int
)

private fun buildCityStats(events: List<Event>, projects: List<Project>): List<MapCityStat> {
    val cityNames = (events.map { it.il } + projects.flatMap { it.katilimciIller })
        .map { normalizeMapCity(it) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    return cityNames.map { city ->
        MapCityStat(
            city = city,
            eventsCount = events.count { normalizeMapCity(it.il) == city },
            projectsCount = projects.count { project -> project.katilimciIller.any { normalizeMapCity(it) == city } }
        )
    }.sortedWith(compareByDescending<MapCityStat> { it.eventsCount + it.projectsCount }.thenBy { it.city })
}

private fun buildMapStateJson(cityStats: List<MapCityStat>, selectedCity: String): String {
    val cities = JSONArray()
    cityStats.forEach { stat ->
        cities.put(
            JSONObject()
                .put("city", stat.city)
                .put("eventsCount", stat.eventsCount)
                .put("projectsCount", stat.projectsCount)
        )
    }
    return JSONObject()
        .put("selectedCity", normalizeMapCity(selectedCity))
        .put("cities", cities)
        .toString()
}

private fun normalizeMapCity(city: String): String {
    return city.trim().replace(Regex("\\s*\\((Asya|Avrupa)\\)$", RegexOption.IGNORE_CASE), "")
}

private const val AtlasSvgPlaceholder = "<!-- ATLAS_TURKEY_SVG -->"

private fun loadAtlasMapHtml(context: Context): String {
    return runCatching {
        val html = context.assets.open("map.html").bufferedReader().use { it.readText() }
        val svg = context.assets.open("map/turkey.svg").bufferedReader().use { it.readText() }
        html.replace(AtlasSvgPlaceholder, svg)
    }.getOrElse {
        """
        <!DOCTYPE html>
        <html lang="tr">
        <body>
        <script>
            if (window.AndroidInterface && window.AndroidInterface.onMapError) {
                window.AndroidInterface.onMapError("Harita assetleri okunamadı");
            }
        </script>
        </body>
        </html>
        """.trimIndent()
    }
}
