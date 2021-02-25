package ru.dgis.sdk.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.SwitchCompat
import java9.util.concurrent.CompletableFuture
import ru.dgis.sdk.context.Context
import ru.dgis.sdk.coordinates.Arcdegree
import ru.dgis.sdk.coordinates.GeoPoint
import ru.dgis.sdk.map.*
import ru.dgis.sdk.map.Map

private const val DIMENSION_ATTR = "is_2d"

fun getPoint (lan: Double, lot: Double) : GeoPoint {
    return GeoPoint(Arcdegree(lan), Arcdegree(lot))
}

fun getPoints() : List<GeoPoint> = listOf(
    getPoint(55.53739580689267, 37.66256779432297),
    getPoint(55.72400750556595, 37.79786495491862),
    getPoint(55.7984068608551, 37.47958129271865),
    getPoint(55.8649130408431, 37.77402866631746),
    getPoint(55.89121083673136, 37.82323840074241),
    getPoint(55.57043582413208, 37.441149428486824),
    getPoint(55.60650179489436, 37.71176059730351),
    getPoint(55.64512768391863, 37.80017928220332),
    getPoint(55.662478298114024, 37.79479038901627),
    getPoint(55.67895765839564, 37.86552191711962),
    getPoint(55.68242877178772, 37.787104016169906),
    getPoint(55.70018593279514, 37.56876013241708),
    getPoint(55.71837485931056, 37.63104513287544),
    getPoint(55.74306440383858, 37.70946311764419),
    getPoint(55.759070448307966, 37.81555202789605),
    getPoint(55.77030621423892, 37.6402688305825),
    getPoint(55.77983300860179, 37.79556739144027),
    getPoint(55.78371738427256, 37.77095410041511),
    getPoint(55.78415647267489, 37.73867128416896),
    getPoint(55.79192392771075, 37.441149428486824),
    getPoint(55.80618843260796, 37.435760451480746),
    getPoint(55.816128229350724, 37.72559601813555),
    getPoint(55.82000898617757, 37.45498484931886),
    getPoint(55.83814210402061, 37.48419309966266),
    getPoint(55.84375741825776, 37.761730402708054),
    getPoint(55.85454741803957, 37.75250678882003),
    getPoint(55.88086906385545, 37.48189562000334),
    getPoint(55.90242536833114, 37.386567648500204),
    getPoint(55.920520053981384, 37.46420854702592),
    getPoint(55.950227539812964, 37.68484982661903),
    getPoint(55.553486118464704, 37.5579992774874),
    getPoint(55.691960072144575, 37.49265655875206),
    getPoint(55.74825400089946, 37.79325306415558),
    getPoint(55.78631800125226, 37.7140749245882),
    getPoint(55.78890152615578, 37.54570101387799),
    getPoint(55.809648199608844, 37.76941685937345),
    getPoint(55.81958715998664, 37.52110465429723),
    getPoint(55.85584538331202, 37.428851164877415),
    getPoint(55.96056762364104, 37.53416298888624),
    getPoint(55.605775104277264, 37.51693516038358),
    getPoint(55.645568332898335, 37.61873002164066))


class GenericMapActivity : AppCompatActivity() {
    lateinit var sdkContext: Context

    private var map: Map? = null
    private var myLocationMapSource: MyLocationMapObjectSource? = null
    private var markersSource: Source? = null

    private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdkContext = (applicationContext as Application).sdkContext

        setContentView(R.layout.activity_map_generic)

        mapView = findViewById<MapView>(R.id.mapView).also {
            it.getMapAsync(this::onMapReady)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        myLocationMapSource?.close()
        markersSource?.close()
        map?.close()
    }

    private fun onMapReady(map: Map) {
        this.map = map

        val gestureManager = checkNotNull(mapView.gestureManager)
        subscribeGestureSwitches(gestureManager)
        setupMapAttributes(map)

        myLocationMapSource = createMyLocationMapObjectSource(
            sdkContext,
            MyLocationDirectionBehaviour.FOLLOW_MAGNETIC_HEADING).also {
                map.addSource(it)
        }

        CompletableFuture
            .supplyAsync {
                /*
                подготовка стиля маркера может быть долгой т.к. требуется растеризация картинки и
                лучше унести ее из UI потока
                 */
                val markerStyle = MarkerStyle(ImageProvider.fromResource(R.drawable.ic_scooter))
                val markers = getPoints().map { position ->
                    MarkerBuilder().run {
                        setPosition(position)
                        setStyle(markerStyle)
                        build()
                    }
                }

                val source = GeometryMapObjectSourceBuilder(sdkContext).run {
                    /*
                    этой шняги тут быть не должно. Нужно выкосить ее и настраивать внешний вид через
                    online редактор стилей
                     */
                    setSourceAttribute("db_sublayer", "s_dynamic_poi")
                    setSourceAttribute("font_color", 4284481689)
                    setSourceAttribute("halo_color", 3774211839)
                    setSourceAttribute("halo_radius_zpt", 0.8)
                    setSourceAttribute("font_size_zpt", 5)
                    setSourceAttribute("icon_width_zpt", 25.0)
                    setSourceAttribute("db_icon_priority", 65000)
                    setSourceAttribute("object_group_priority", 65000)

                    addObjects(markers)
                    createSourceWithClustering(ScreenDistance(10f), Zoom(18f))
                }
                map.addSource(source)

                source
            }
            .thenAcceptAsync({
                this.markersSource = it
            }, mainExecutor)
    }

    private fun subscribeGestureSwitches(gm: GestureManager) {
        val enabledGestures = gm.enabledGestures()
        val options = listOf(
            Pair(R.id.rotationSwitch, Gesture.ROTATION),
            Pair(R.id.shiftSwitch, Gesture.SHIFT),
            Pair(R.id.scaleSwitch, Gesture.SCALING),
            Pair(R.id.tiltSwitch, Gesture.TILT),
        )

        options.forEach { (viewId, gesture) ->
            findViewById<SwitchCompat>(viewId).apply {
                isEnabled = true
                isChecked = enabledGestures.contains(gesture)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked)
                        gm.enableGesture(gesture)
                    else
                        gm.disableGesture(gesture)
                }
            }
        }
    }

    private fun setupMapAttributes(map: Map) {
        findViewById<SwitchCompat>(R.id.flatModeSwitch).apply {
            isEnabled = true

            setOnCheckedChangeListener { _, isChecked ->
                map.setStyleAttribute(DIMENSION_ATTR, isChecked)
            }
        }
    }
}
