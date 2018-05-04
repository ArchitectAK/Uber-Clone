package com.cogitator.ubercaranimation

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.JointType.ROUND
import org.json.JSONObject


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 03/05/2018 (MM/DD/YYYY)
 */
class MainActivity : FragmentActivity(), OnMapReadyCallback {


    private lateinit var roorkee: LatLng

    private lateinit var polylineOptions: PolylineOptions


    private lateinit var greyPolyLine: Polyline

    private lateinit var blackPolylineOptions: PolylineOptions

    private lateinit var blackPolyline: Polyline

    private lateinit var marker: Marker

    private lateinit var startPosition: LatLng

    private lateinit var endPosition: LatLng

    private var v: Float = 0.toFloat()
    private var lat: Double = 0.toDouble()
    private var lng: Double = 0.toDouble()
    private var index: Int = 0
    private var next: Int = 0

    private var polyLineList: MutableList<LatLng> = ArrayList()

    private lateinit var destination: String

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        val button: Button = findViewById(R.id.destination_button)
        val editText: EditText = findViewById(R.id.edittext_place)
        button.setOnClickListener {
            destination = editText.text.toString()
            destination = destination.replace(" ", "+")
            mapFragment.getMapAsync(this@MainActivity)
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val latitude = 3.1390
        val longitude = 101.6869
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.isTrafficEnabled = false
        mMap.isIndoorEnabled = false
        mMap.isBuildingsEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = true
        // Add a marker in Home and move the camera
        roorkee = LatLng(3.1390, 101.6869)
        mMap.addMarker(MarkerOptions().position(roorkee).title("Marker in Home"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(roorkee))
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(googleMap.cameraPosition.target)
                .zoom(17f)
                .bearing(30f)
                .tilt(45f)
                .build()))
        var requestUrl: String? = null
        try {
            requestUrl = ("https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"
                    + "transit_routing_preference=less_driving&"
                    + "origin=" + latitude + "," + longitude + "&"
                    + "destination=" + destination + "&"
                    + "key=" + resources.getString(R.string.google_directions_key))
//            Log.d(FragmentActivity.TAG, requestUrl)
            val jsonObjectRequest = JsonObjectRequest(Request.Method.GET,
                    requestUrl, null,
                    Response.Listener<JSONObject> { response ->
                        //                        Log.d(TAG, response.toString() + "")
                        try {
                            val jsonArray = response.getJSONArray("routes")
                            for (i in 0 until jsonArray.length()) {
                                val route = jsonArray.getJSONObject(i)
                                val poly = route.getJSONObject("overview_polyline")
                                val polyline = poly.getString("points")
                                polyLineList = Utils().decodePoly(polyline)
//                                Log.d(FragmentActivity.TAG, polyLineList + "")
                            }
                            //Adjusting bounds
                            val builder = LatLngBounds.Builder()
                            for (latLng in polyLineList) {
                                builder.include(latLng)
                            }
                            val bounds = builder.build()
                            val mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2)
                            mMap.animateCamera(mCameraUpdate)

                            polylineOptions = PolylineOptions()
                            polylineOptions.color(Color.GRAY)
                            polylineOptions.width(5F)
                            polylineOptions.startCap(SquareCap())
                            polylineOptions.endCap(SquareCap())
                            polylineOptions.jointType(ROUND)
                            polylineOptions.addAll(polyLineList)

                            greyPolyLine = mMap.addPolyline(polylineOptions)

                            blackPolylineOptions = PolylineOptions()
                            blackPolylineOptions.width(5F)
                            blackPolylineOptions.color(Color.BLACK)
                            blackPolylineOptions.startCap(SquareCap())
                            blackPolylineOptions.endCap(SquareCap())
                            blackPolylineOptions.jointType(ROUND)
                            blackPolyline = mMap.addPolyline(blackPolylineOptions)

                            mMap.addMarker(MarkerOptions()
                                    .position(polyLineList[polyLineList.size - 1]))

                            val polylineAnimator = ValueAnimator.ofInt(0, 100)
                            polylineAnimator.duration = 2000
                            polylineAnimator.interpolator = LinearInterpolator()
                            polylineAnimator.addUpdateListener { valueAnimator ->
                                val points = greyPolyLine.points
                                val percentValue = valueAnimator.animatedValue as Int
                                val size = points.size
                                val newPoints = (size * (percentValue / 100.0f)).toInt()
                                val p = points.subList(0, newPoints)
                                blackPolyline.points = p
                            }
                            polylineAnimator.start()
                            marker = mMap.addMarker(MarkerOptions().position(roorkee)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_motorcycle_black_24dp)))

                            index = -1
                            next = 1
                            Handler().postDelayed(object : Runnable {
                                override fun run() {
                                    if (index < polyLineList.size - 1) {
                                        index++
                                        next = index + 1
                                    }
                                    if (index < polyLineList.size - 1) {
                                        startPosition = polyLineList[index]
                                        endPosition = polyLineList[next]
                                    }
                                    val valueAnimator = ValueAnimator.ofFloat(0F, 1F)
                                    valueAnimator.duration = 3000
                                    valueAnimator.interpolator = LinearInterpolator()
                                    valueAnimator.addUpdateListener { valueAnimator ->
                                        v = valueAnimator.animatedFraction
                                        lng = v * endPosition.longitude + (1 - v) * startPosition.longitude
                                        lat = v * endPosition.latitude + (1 - v) * startPosition.latitude
                                        val newPos = LatLng(lat, lng)
                                        marker.setPosition(newPos)
                                        marker.setAnchor(0.5f, 0.5f)
                                        marker.rotation = getBearing(startPosition, newPos)
                                        mMap.moveCamera(CameraUpdateFactory
                                                .newCameraPosition(CameraPosition.Builder()
                                                        .target(newPos)
                                                        .zoom(15.5f)
                                                        .build()))
                                    }
                                    valueAnimator.start()
                                    Handler().postDelayed(this, 3000)
                                }
                            }, 3000)


                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, Response.ErrorListener { error -> Log.d("TAG", error.toString() + "") })

            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(jsonObjectRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

//    private fun decodePoly(encoded: String): List<LatLng> {
//        val poly: MutableList<LatLng> = ArrayList()
//        var index = 0
//        var len = encoded.length
//        var lat = 0
//        var lng = 0
//
//        while (index < len) {
//            var b = 0
//            var shift = 0
//            var result = 0
//            do {
//                b = (encoded[index++] - 63).toInt()
//                result | = (b & 0x1f) shl(shift)
//                shift += 5
//            } while (b >= 0x20)
//            val dlat = ((result & 1) != 0 ? ~(result shr(1)) : (result shr(1)))
//            lat += dlat
//
//            shift = 0
//            result = 0
//            do {
//                b = (encoded[index++] - 63).toInt()
//                result | = (b & 0x1f) shl(shift)
//                shift += 5
//            } while (b >= 0x20)
//            val dlng = 0
//            if ((result & 1) != 0)
//            dlng ~(result shr(1))
//            else dlng(result shr(1))
//            lng += dlng
//
//            val p = LatLng(((lat.toDouble() / 1E5)),
//                    ((lng.toDouble() / 1E5)))
//            poly.add(p)
//        }
//
//        return poly
//    }

    fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat: Double = Math.abs(begin.latitude - end.latitude)
        val lng: Double = Math.abs(begin.longitude - end.longitude)

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (Math.toDegrees(Math.atan(lng / lat)).toFloat())
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (((90 - Math.toDegrees(Math.atan(lng / lat))) + 90).toFloat())
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return ((Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat())
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (((90 - Math.toDegrees(Math.atan(lng / lat))) + 270).toFloat())
        return -1F
    }
}