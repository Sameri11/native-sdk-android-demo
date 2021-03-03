package ru.dgis.sdk.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java9.util.concurrent.CompletableFuture
import ru.dgis.sdk.DGis
import ru.dgis.sdk.map.Map
import ru.dgis.sdk.map.MapView
import ru.dgis.sdk.map.Source
import ru.dgis.sdk.map.createTrafficSource

class TrafficScoreActivity : AppCompatActivity() {

    private var map: Map? = null
    private var source: Source? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_traffic_score)

        val mapView = findViewById<MapView>(R.id.mapView)
        mapView.getMapAsync(this::onMapReady)
        lifecycle.addObserver(mapView)
    }

    private fun onMapReady(map: Map) {
        this.map = map

        CompletableFuture
            .supplyAsync {
                // todo: fixme context
                val source = createTrafficSource(DGis.context())
                map.addSource(source)
                source
            }
            .thenAcceptAsync({
                             // todo: weak ptr here
                             source = it
            }, mainExecutor)
    }

    override fun onDestroy() {
        super.onDestroy()

        source?.close()
        map?.close()
    }
}