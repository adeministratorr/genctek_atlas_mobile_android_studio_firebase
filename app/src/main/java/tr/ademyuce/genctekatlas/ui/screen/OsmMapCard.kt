package tr.ademyuce.genctekatlas.ui.screen

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import tr.ademyuce.genctekatlas.data.model.Event
import tr.ademyuce.genctekatlas.data.model.Project
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsmMapCard(
    events: List<Event>,
    projects: List<Project>,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    onEventSelected: (Event) -> Unit = {},
    onProjectSelected: (Project) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expandedCity by remember(events, projects) { mutableStateOf<String?>(null) }
    var selectedMarker by remember(events, projects) { mutableStateOf<OsmMarker?>(null) }
    val markers = remember(events, projects, expandedCity) {
        buildOsmMarkers(events, projects, expandedCity)
    }

    LaunchedEffect(selectedCity) {
        if (selectedCity.isBlank()) {
            expandedCity = null
            selectedMarker = null
        }
    }

    MobileCard(modifier = modifier) {
        Text(
            text = "OpenStreetMap",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            AndroidView(
                factory = { context ->
                    Configuration.getInstance().setUserAgentValue(context.packageName)
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 4.0
                        maxZoomLevel = 18.0
                        controller.setZoom(5.2)
                        controller.setCenter(GeoPoint(39.0, 35.0))
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    markers
                        .sortedWith(
                            compareBy<OsmMarker> { selectedCity.equals(it.city, ignoreCase = true) }
                                .thenBy { it.totalCount }
                        )
                        .forEach { markerItem ->
                            mapView.overlays.add(
                                Marker(mapView).apply {
                                    position = GeoPoint(markerItem.latitude, markerItem.longitude)
                                    title = markerItem.title
                                    snippet = markerItem.snippet
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    setIcon(
                                        createClusterPinDrawable(
                                            resources = mapView.context.resources,
                                            count = markerItem.totalCount,
                                            selected = selectedCity.equals(markerItem.city, ignoreCase = true)
                                        )
                                    )
                                    setOnMarkerClickListener { marker, _ ->
                                        expandedCity = markerItem.city
                                        selectedMarker = if (markerItem.isCluster && markerItem.totalCount > 1) {
                                            null
                                        } else {
                                            markerItem
                                        }
                                        onCitySelected(markerItem.city)
                                        val target = GeoPoint(markerItem.latitude, markerItem.longitude)
                                        val targetZoom = if (markerItem.isCluster && markerItem.totalCount > 1) {
                                            mapView.zoomLevelDouble.coerceAtLeast(8.6)
                                        } else {
                                            mapView.zoomLevelDouble.coerceAtLeast(11.0)
                                        }
                                        mapView.controller.animateTo(target)
                                        mapView.controller.setZoom(targetZoom)
                                        true
                                    }
                                }
                            )
                        }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        selectedMarker?.let { marker ->
            OsmMarkerInfoPanel(
                marker = marker,
                onClick = {
                    marker.event?.let(onEventSelected)
                    marker.project?.let(onProjectSelected)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OsmMarkerInfoPanel(
    marker: OsmMarker,
    onClick: () -> Unit
) {
    val canOpenDetail = marker.event != null || marker.project != null
    Surface(
        shape = MobileCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpenDetail) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = marker.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetaChip(text = marker.city, icon = Icons.Default.LocationOn)
                marker.event?.tarih?.takeIf { it.isNotBlank() }?.let {
                    MetaChip(text = it, icon = Icons.Default.CalendarToday)
                }
                marker.event?.tema?.takeIf { it.isNotBlank() }?.let {
                    MetaChip(text = it, icon = Icons.Default.Label)
                }
                marker.project?.tema?.takeIf { it.isNotBlank() }?.let {
                    MetaChip(text = it, icon = Icons.Default.Code)
                }
            }
            Text(
                text = marker.event?.aciklama?.takeIf { it.isNotBlank() }
                    ?: marker.project?.aciklama?.takeIf { it.isNotBlank() }
                    ?: marker.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (canOpenDetail) {
                Text(
                    text = "Detayı açmak için dokunun",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun createClusterPinDrawable(
    resources: Resources,
    count: Int,
    selected: Boolean
): Drawable {
    val density = resources.displayMetrics.density
    fun dp(value: Float): Float = value * density

    val width = dp(34f).roundToInt()
    val height = dp(44f).roundToInt()
    val centerX = width / 2f
    val centerY = dp(16.5f)
    val radius = dp(13.5f)
    val pointY = height - dp(3f)
    val pinColor = if (selected) Color.rgb(183, 28, 28) else Color.rgb(211, 47, 47)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val pinPath = Path().apply {
        moveTo(centerX, pointY)
        lineTo(centerX - radius * 0.62f, centerY + radius * 0.58f)
        arcTo(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            132f,
            276f,
            false
        )
        close()
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(64, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.translate(dp(1.4f), dp(2f))
    canvas.drawPath(pinPath, shadowPaint)
    canvas.restore()

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(1.6f)
    }
    canvas.drawPath(pinPath, borderPaint)

    val label = count.coerceAtLeast(1).let { if (it > 99) "99+" else it.toString() }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = if (label.length > 2) dp(9f) else dp(12f)
    }
    val textBounds = Rect()
    textPaint.getTextBounds(label, 0, label.length, textBounds)
    canvas.drawText(label, centerX, centerY - textBounds.exactCenterY(), textPaint)

    return BitmapDrawable(resources, bitmap)
}

private data class OsmMarker(
    val id: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val eventCount: Int,
    val projectCount: Int,
    val title: String,
    val snippet: String,
    val isCluster: Boolean,
    val event: Event? = null,
    val project: Project? = null
) {
    val totalCount: Int get() = eventCount + projectCount
}

private data class OsmPoint(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val isEvent: Boolean,
    val title: String,
    val event: Event? = null,
    val project: Project? = null
)

private fun buildOsmMarkers(
    events: List<Event>,
    projects: List<Project>,
    expandedCity: String?
): List<OsmMarker> {
    val points = buildList {
        events.forEach { event ->
            val city = normalizeOsmCity(event.il)
            val coordinates = event.enlem?.let { lat ->
                event.boylam?.let { lon -> lat to lon }
            } ?: cityCoordinates(city)
            if (city.isNotBlank() && coordinates != null) {
                add(
                    OsmPoint(
                        city = city,
                        latitude = coordinates.first,
                        longitude = coordinates.second,
                        isEvent = true,
                        title = event.ad.ifBlank { city },
                        event = event
                    )
                )
            }
        }
        projects.forEach { project ->
            project.katilimciIller.forEach { rawCity ->
                val city = normalizeOsmCity(rawCity)
                val coordinates = cityCoordinates(city)
                if (city.isNotBlank() && coordinates != null) {
                    add(
                        OsmPoint(
                            city = city,
                            latitude = coordinates.first,
                            longitude = coordinates.second,
                            isEvent = false,
                            title = project.ad.ifBlank { project.takimAdi.ifBlank { city } },
                            project = project
                        )
                    )
                }
            }
        }
    }

    return points
        .groupBy { it.city }
        .flatMap { (city, cityPoints) ->
            when {
                cityPoints.size == 1 -> listOf(cityPoints.first().toItemMarker(city, 0))
                expandedCity.equals(city, ignoreCase = true) -> cityPoints.spreadAroundCity(city)
                else -> listOfNotNull(cityPoints.toCityMarker(city))
            }
        }
        .sortedWith(compareByDescending<OsmMarker> { it.totalCount }.thenBy { it.city })
}

private fun List<OsmPoint>.toCityMarker(city: String): OsmMarker? {
    val first = firstOrNull() ?: return null
    val latitude = map { it.latitude }.average().takeIf { !it.isNaN() } ?: first.latitude
    val longitude = map { it.longitude }.average().takeIf { !it.isNaN() } ?: first.longitude
    val eventCount = count { it.isEvent }
    val projectCount = count { !it.isEvent }
    return OsmMarker(
        id = "cluster-$city",
        city = city,
        latitude = latitude,
        longitude = longitude,
        eventCount = eventCount,
        projectCount = projectCount,
        title = "$city (${eventCount + projectCount})",
        snippet = "$eventCount etkinlik, $projectCount proje",
        isCluster = true
    )
}

private fun List<OsmPoint>.spreadAroundCity(city: String): List<OsmMarker> {
    val centerLatitude = map { it.latitude }.average().takeIf { !it.isNaN() } ?: first().latitude
    val centerLongitude = map { it.longitude }.average().takeIf { !it.isNaN() } ?: first().longitude
    val size = size.coerceAtLeast(1)
    return mapIndexed { index, point ->
        val angle = (2.0 * PI * index) / size
        val ring = 0.10 + (index / 10) * 0.045
        point.toItemMarker(
            city = city,
            index = index,
            latitude = centerLatitude + sin(angle) * ring,
            longitude = centerLongitude + cos(angle) * ring
        )
    }
}

private fun OsmPoint.toItemMarker(
    city: String,
    index: Int,
    latitude: Double = this.latitude,
    longitude: Double = this.longitude
): OsmMarker {
    val stableId = event?.id?.takeIf { it.isNotBlank() }
        ?: project?.id?.takeIf { it.isNotBlank() }
        ?: "$city-$index"
    return OsmMarker(
        id = "item-$stableId",
        city = city,
        latitude = latitude,
        longitude = longitude,
        eventCount = if (isEvent) 1 else 0,
        projectCount = if (isEvent) 0 else 1,
        title = title,
        snippet = city,
        isCluster = false,
        event = event,
        project = project
    )
}

private fun normalizeOsmCity(city: String): String {
    return city.trim().replace(Regex("\\s*\\((Asya|Avrupa)\\)$", RegexOption.IGNORE_CASE), "")
}

private fun cityCoordinates(city: String): Pair<Double, Double>? {
    return turkeyCityCoordinates[city.lowercaseTurkish()]
}

private fun String.lowercaseTurkish(): String {
    return lowercase()
        .replace("ı", "i")
        .replace("ğ", "g")
        .replace("ü", "u")
        .replace("ş", "s")
        .replace("ö", "o")
        .replace("ç", "c")
}

private val turkeyCityCoordinates = mapOf(
    "adana" to (37.0000 to 35.3213),
    "adiyaman" to (37.7648 to 38.2786),
    "afyonkarahisar" to (38.7569 to 30.5387),
    "agri" to (39.7191 to 43.0503),
    "amasya" to (40.6533 to 35.8330),
    "ankara" to (39.9334 to 32.8597),
    "antalya" to (36.8969 to 30.7133),
    "artvin" to (41.1828 to 41.8183),
    "aydin" to (37.8444 to 27.8458),
    "balikesir" to (39.6484 to 27.8826),
    "bilecik" to (40.1500 to 29.9833),
    "bingol" to (38.8847 to 40.4939),
    "bitlis" to (38.4000 to 42.1167),
    "bolu" to (40.7395 to 31.6116),
    "burdur" to (37.7203 to 30.2908),
    "bursa" to (40.1826 to 29.0665),
    "canakkale" to (40.1553 to 26.4142),
    "cankiri" to (40.6013 to 33.6134),
    "corum" to (40.5506 to 34.9556),
    "denizli" to (37.7765 to 29.0864),
    "diyarbakir" to (37.9144 to 40.2306),
    "edirne" to (41.6771 to 26.5557),
    "elazig" to (38.6743 to 39.2225),
    "erzincan" to (39.7500 to 39.5000),
    "erzurum" to (39.9000 to 41.2700),
    "eskisehir" to (39.7767 to 30.5206),
    "gaziantep" to (37.0662 to 37.3833),
    "giresun" to (40.9128 to 38.3895),
    "gumushane" to (40.4603 to 39.4814),
    "hakkari" to (37.5833 to 43.7333),
    "hatay" to (36.4018 to 36.3498),
    "isparta" to (37.7648 to 30.5566),
    "mersin" to (36.8121 to 34.6415),
    "istanbul" to (41.0082 to 28.9784),
    "izmir" to (38.4237 to 27.1428),
    "kars" to (40.6013 to 43.0975),
    "kastamonu" to (41.3887 to 33.7827),
    "kayseri" to (38.7205 to 35.4826),
    "kirklareli" to (41.7351 to 27.2252),
    "kirsehir" to (39.1425 to 34.1709),
    "kocaeli" to (40.8533 to 29.8815),
    "konya" to (37.8746 to 32.4932),
    "kutahya" to (39.4167 to 29.9833),
    "malatya" to (38.3552 to 38.3095),
    "manisa" to (38.6191 to 27.4289),
    "kahramanmaras" to (37.5753 to 36.9228),
    "mardin" to (37.3212 to 40.7245),
    "mugla" to (37.2153 to 28.3636),
    "mus" to (38.7432 to 41.5065),
    "nevsehir" to (38.6244 to 34.7239),
    "nigde" to (37.9667 to 34.6833),
    "ordu" to (40.9839 to 37.8764),
    "rize" to (41.0201 to 40.5234),
    "sakarya" to (40.7569 to 30.3781),
    "samsun" to (41.2867 to 36.33),
    "siirt" to (37.9333 to 41.95),
    "sinop" to (42.0264 to 35.1551),
    "sivas" to (39.7477 to 37.0179),
    "tekirdag" to (40.9780 to 27.5110),
    "tokat" to (40.3167 to 36.55),
    "trabzon" to (41.0015 to 39.7178),
    "tunceli" to (39.1079 to 39.5401),
    "sanliurfa" to (37.1591 to 38.7969),
    "usak" to (38.6823 to 29.4082),
    "van" to (38.4891 to 43.4089),
    "yozgat" to (39.8181 to 34.8147),
    "zonguldak" to (41.4564 to 31.7987),
    "aksaray" to (38.3687 to 34.0370),
    "bayburt" to (40.2552 to 40.2249),
    "karaman" to (37.1759 to 33.2287),
    "kirikkale" to (39.8468 to 33.5153),
    "batman" to (37.8812 to 41.1351),
    "sirnak" to (37.5164 to 42.4611),
    "bartin" to (41.5811 to 32.4610),
    "ardahan" to (41.1105 to 42.7022),
    "igdir" to (39.9237 to 44.0450),
    "yalova" to (40.6500 to 29.2667),
    "karabuk" to (41.2061 to 32.6204),
    "kilis" to (36.7165 to 37.1147),
    "osmaniye" to (37.0742 to 36.2478),
    "duzce" to (40.8438 to 31.1565)
)
