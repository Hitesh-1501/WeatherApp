 package com.example.weatherapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.weatherService
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.Call
import retrofit.Callback
import retrofit.GsonConverterFactory
import retrofit.Response
import retrofit.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

 class MainActivity : AppCompatActivity() {
    private var binding : ActivityMainBinding? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.lottieAnimationView?.setFailureListener{
            Log.e("Lootle","Failed to load animation")
        }


        binding?.lottieAnimationView?.setAnimation(R.raw.cloudanimation)
        binding?.lottieAnimationView?.loop(true)
        binding?.lottieAnimationView?.playAnimation()


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Initialize the SharedPreferences variable
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        // TODO  Call the UI method to populate the data in
        //  the UI which are already stored in sharedPreferences earlier.
        //  At first run it will be blank.)
        setupUI()

        //: Check here whether GPS is ON or OFF using the method which we have created
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this).withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                @RequiresApi(Build.VERSION_CODES.S)
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if(report!!.areAllPermissionsGranted()) {
                        requestLocationData()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()
        }

    }

    private fun isLocationEnabled(): Boolean {
        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

     private fun showRationalDialogForPermission() {
         AlertDialog.Builder(this).setMessage(
             "" +
                     "it looks like you have turned off permission required" +
                     "for this feature it can be enabled under the" +
                     "Applications Settings"
            )
             .setPositiveButton("GO TO SETTINGS") { _, _ ->
                 try {
                     val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                     val uri = Uri.fromParts("package", packageName, null)
                     intent.data = uri
                     startActivity(intent)
                 } catch (e: ActivityNotFoundException) {
                     e.printStackTrace()
                 }
             }
             .setNegativeButton("Cancle") { dialog, _ ->
                 dialog.dismiss()
             }.show()

     }
     /**
      * A function to request the current location. Using the fused location provider client.
      */
     @SuppressLint("MissingPermission")
     private fun requestLocationData(){
         val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,0L).apply {
             setWaitForAccurateLocation(false)
             setMinUpdateIntervalMillis(1000)
             setMaxUpdateDelayMillis(0)
             setMaxUpdates(1)
         }.build()
         mFusedLocationClient.requestLocationUpdates(
             locationRequest,mLocationCallBack, Looper.myLooper()
         )
     }

     //Create a location callback object of fused location provider client where we will get the current location details
     private val mLocationCallBack = object : LocationCallback() {
         override fun onLocationResult(locationResult: LocationResult) {
             val mLastLocation: Location? = locationResult.lastLocation
             val latitude = mLastLocation!!.latitude
             Log.e("Current Latitude", "$latitude")
             val longitude = mLastLocation.longitude
             Log.e("Current Longitude", "$longitude")
             getLocationWeatherDetails(latitude,longitude)
         }
     }
     /**
      * Function is used to get the weather details of the current location based on the latitude longitude
      */

     private fun getLocationWeatherDetails(latitude: Double, longitude: Double){
         if(Constants.isNetworkAvailable(this)){
             // TODO  Make an api call using retrofit.
             // START
             /**
              * Add the built-in converter factory first. This prevents overriding its
              * behavior but also ensures correct behavior when using converters that consume all types.
              */
             val retrofit: Retrofit = Retrofit.Builder()
                 // API base URL.
                 .baseUrl(Constants.BASE_URL)
                 /** Add converter factory for serialization and deserialization of objects. */
                 .addConverterFactory(GsonConverterFactory.create())
                 /** Create the Retrofit instances. */
                 .build()

             // TODO STEP FOR API CALL
             /**
              * Here we map the service interface in which we declares the end point and the API type
              *i.e GET, POST and so on along with the request parameter which are required.
              */
             val service: weatherService =
                 retrofit.create<weatherService>(weatherService::class.java)

             /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
              * Here we pass the required param in the service
              */
             val listCall: Call<WeatherResponse> = service.getWeather(
                 latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
             )
             showProgressDialog()
             // Callback methods are executed using the Retrofit callback executor.
             listCall.enqueue(object: Callback<WeatherResponse>{
                 override fun onResponse(
                     response: Response<WeatherResponse>?,
                     retrofit: Retrofit?
                 ){
                     // Check weather the response is success or not.
                     if (response!!.isSuccess) {
                         hideProgressDialog()
                         /** The de-serialized response body of a successful response. */
                         // TODO Here we convert the response object to string and store the string in the SharedPreference.
                         val weatherList: WeatherResponse = response.body()
                         Log.i("Response Result", "$weatherList")
                         // Here we have converted the model class in to Json String to store it in the SharedPreferences.
                         val weatherResponseJsonString = Gson().toJson(weatherList)
                         val editor = mSharedPreferences.edit()
                         editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                         editor.apply()
                         setupUI()

                     } else {
                         // If the response is not success then we check the response code.
                         val sc = response.code()
                         when (sc) {
                             400 -> {
                                 Log.e("Error 400", "Bad Request")
                             }
                             404 -> {
                                 Log.e("Error 404", "Not Found")
                             }
                             else -> {
                                 Log.e("Error", "Generic Error")
                             }
                         }
                     }

                 }
                 override fun onFailure(t: Throwable?) {
                     Log.e("Errorrrrr", t!!.message.toString())
                     hideProgressDialog()
                 }

             })
         }else{
             Toast.makeText(
                 this@MainActivity,
                 "No internet connection available.",
                 Toast.LENGTH_SHORT
             ).show()

         }
     }
     private fun showProgressDialog(){
         mProgressDialog = Dialog(this@MainActivity)
         mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
         mProgressDialog!!.show()
     }
     private fun hideProgressDialog(){
         if(mProgressDialog != null){
             mProgressDialog!!.dismiss()
         }

     }

     override fun onCreateOptionsMenu(menu: Menu?): Boolean {
         menuInflater.inflate(R.menu.menu_main,menu)
         return super.onCreateOptionsMenu(menu)
     }

     override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return when(item.itemId){
             R.id.action_refresh->{
                 requestLocationData()
                 true
             }else -> super.onOptionsItemSelected(item)
         }

     }

     @SuppressLint("SetTextI18n")
     private fun setupUI(){
         // TODO Here we get the stored response from
         //  SharedPreferences and again convert back to data object
         //  to populate the data in the UI.
         // Here we have got the latest stored response from the SharedPreference and converted back to the data model object.
         val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
         if(!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
             for(i in weatherList.weather.indices){
                 Log.i("Weather Name",weatherList.weather.toString())
                 binding?.tvMain?.text = weatherList.weather[i].main
                 binding?.tvMainDescription?.text = weatherList.weather[i].description
                 binding?.tvTemp?.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                 binding?.tvHumidity?.text = weatherList.main.humidity.toString() + "per cent"
                 binding?.tvMin?.text = weatherList.main.temp_min.toString() + " min"
                 binding?.tvMax?.text = weatherList.main.temp_max.toString() + " max"
                 binding?.tvSpeed?.text = weatherList.wind.speed.toString()
                 binding?.tvName?.text = weatherList.name
                 binding?.tvCountry?.text = weatherList.sys.country
                 binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise)
                 binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset)

                 // Here we update the main icon
                 val ivMain = binding?.ivMain
                 when (weatherList.weather[i].icon) {
                     "01d" -> ivMain?.setImageResource(R.drawable.sun)
                     "02d" -> ivMain?.setImageResource(R.drawable.fewcloudssun)
                     "03d" -> ivMain?.setImageResource(R.drawable.scatteredcloudsday)
                     "04d" -> ivMain?.setImageResource(R.drawable.brokenclouds)
                     "09d" -> ivMain?.setImageResource(R.drawable.showerrain)
                     "10d" -> ivMain?.setImageResource(R.drawable.rainday)
                     "11d" -> ivMain?.setImageResource(R.drawable.thunderstrom)
                     "13d" -> ivMain?.setImageResource(R.drawable.snowflake)
                     "50d" -> ivMain?.setImageResource(R.drawable.mistday)
                     "01n" -> ivMain?.setImageResource(R.drawable.clearnight)
                     "02n" -> ivMain?.setImageResource(R.drawable.fewcloudsnight)
                     "03n" -> ivMain?.setImageResource(R.drawable.scatteredcloudsnight)
                     "04n" -> ivMain?.setImageResource(R.drawable.brokenclouds)
                     "09n" -> ivMain?.setImageResource(R.drawable.showerrain)
                     "10n" -> ivMain?.setImageResource(R.drawable.rainnight)
                     "11n" -> ivMain?.setImageResource(R.drawable.thunderstrom)
                     "13n" -> ivMain?.setImageResource(R.drawable.snowflake)
                     "50n" -> ivMain?.setImageResource(R.drawable.mistnight)
                 }
             }
         }
     }
     /**
      * Function is used to get the temperature unit value.
      */
     private fun getUnit(value: String):String?{
         Log.i("unit", value)
         var value = "°C"
         if("US" == value || "LR" == value || "MM" == value){
             value = "°F"
         }
         return value
     }
     /**
      * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
      */
     @SuppressLint("SimpleDateFormat")
     private fun unixTime(timex: Long): String? {
         val date = Date(timex *1000L)
         val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
         sdf.timeZone = TimeZone.getDefault()
         return sdf.format(date)
     }


 }