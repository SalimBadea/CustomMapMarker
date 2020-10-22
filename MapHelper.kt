package com.yisweb.slamtac.helper

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.yisweb.slamtac.R
import com.yisweb.slamtac.helper.SharedPreferencesManager.getLanguage
import com.yisweb.slamtac.model.MapObject
import java.io.IOException
import java.util.*

/**
 * Created by µðšţãƒâ ™ on 11/12/2019.
 *  ->
 */

class MapHelper(
    private val mActivity: Activity,
    private val mMap: GoogleMap
) {
    private val mLocation = mutableListOf<MapObject>()
    private var tempMarker: MapObject? = null
    lateinit var mLatLng: LatLng

    fun getLatLng(latLng: LatLng) {
        mLatLng = latLng
    }

    fun addMarkers(location: MutableList<MapObject>) {
        if (mLocation.size == 0) {
            mLocation.addAll(location)
            location.forEach {
                it.marker = addMarkerDrawable(it, R.drawable.marker_map)
            }
            if (location.isNotEmpty()) {
                changeMarkerColor(location[0])
                showLocation()
            }
        }
    }

    fun onMarkerClicked(marker: (data: Marker) -> Unit) {
        mMap.setOnMarkerClickListener {
            marker(it)
            true
        }
    }

    fun changeMarkerColor(mapObject: MapObject) {
        if (tempMarker != null) {
            tempMarker!!.marker!!.zIndex = 0f
            tempMarker!!.marker!!.setIcon(
                bitmapDescriptor(
                    tempMarker!!.distance,
                    R.drawable.marker_map
                )
            )
        }
        mapObject.marker!!.zIndex = 1f
        mapObject.marker!!.setIcon(
            bitmapDescriptor(
                mapObject.distance,
                R.drawable.marker_map_selected
            )
        )
        tempMarker = mapObject
    }

    private fun myLocationMarker() {
        mMap.addMarker(
            MarkerOptions()
                .position(LatLng(mLatLng.latitude, mLatLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )
    }

    private fun addMarkerDrawable(mapObject: MapObject, @DrawableRes drawable: Int): Marker =
        mMap.addMarker(
            MarkerOptions()
                .position(LatLng(mapObject.latitude.toDouble(), mapObject.longitude.toDouble()))
                .icon(bitmapDescriptor(mapObject.distance, drawable))
        )

    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    private fun showLocation() {
        myLocationMarker()
        val builder = LatLngBounds.Builder()
        builder.include(LatLng(mLatLng.latitude, mLatLng.longitude))
        mLocation.forEach {
            builder.include(it.marker!!.position)
        }
        val bounds = builder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun bitmapDescriptor(distance: Double, @DrawableRes drawable: Int): BitmapDescriptor {
        val conf = Bitmap.Config.ARGB_8888
        val bmp = Bitmap.createBitmap(160, 120, conf)
        val canvas = Canvas(bmp)
        val color = Paint()
        color.textSize = 32f
        color.color = Color.BLACK
        canvas.drawBitmap(
            BitmapFactory.decodeResource(
                mActivity.resources,
                drawable
            ), 0f, 0f, color
        )
        val distanceText = if (distance > 0.99) {
            mActivity.getString(R.string.km, distance)
        } else {
            mActivity.getString(R.string.m, (distance * 1000).toInt())
        }

        canvas.drawText(
            distanceText,
            18f,
            50f,
            color
        )
        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    fun viewAnimatedCamera(latLng: LatLng) {
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(latLng)
                    .zoom(12f)
                    .build()
            ),
            2000, null
        )
    }

    fun getCityNameByCoordinates(lat: Double, lng: Double): String {
        val geoCoder = Geocoder(
            mActivity,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Locale.forLanguageTag(
                getLanguage()
            )
            else Locale.getDefault()
        )
        return try {
            var address = "No Locality Found"
            val addresses = geoCoder.getFromLocation(lat, lng, 20)
            if (addresses != null && addresses.size > 0) {
                addresses.forEach {
                    if (it.locality != null && it.locality.isNotEmpty())
                        address = it.getAddressLine(0)
                }
            }
            address
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }
}