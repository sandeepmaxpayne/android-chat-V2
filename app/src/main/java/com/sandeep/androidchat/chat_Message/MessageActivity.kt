package com.sandeep.androidchat.chat_Message

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.sandeep.androidchat.R
import com.sandeep.androidchat.model.ChatMessage
import com.sandeep.androidchat.model.User
import com.sandeep.androidchat.sign_In_Activity.RegisterActivity
import com.sandeep.androidchat.view.LatestMessageRow
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.activity_notification.*
import kotlinx.android.synthetic.main.nav_header.*
import kotlinx.android.synthetic.main.navigation_drawer.*
import java.util.*
import kotlin.collections.HashMap


class MessageActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    companion object {
        var currentUser: User? = null
        val TAG = "LatestMessage"
    }

    private fun getDeviceName(): String{
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT))) {
            capitalized(model)
        }else{
            capitalized(manufacturer) + " " + model
        }

    }

    private fun capitalized(s: String): String{
        if (s.isEmpty()){
            return "";
        }
        val first = s[0]
        return if (Character.isUpperCase(first)){
            s
        }else{
            Character.toUpperCase(first) + s.substring(1)
        }
    }


    override fun onRestart() {
        super.onRestart()
        listenForLatestMessage()
        fetchCurrentUser()
        verifyUserLoggedIn()
    }
    override fun onNavigationItemSelected(item: MenuItem) = when (item.itemId){
        R.id.nav_home -> {
            if (drawerLayout!!.isDrawerOpen(GravityCompat.START)){
                drawerLayout!!.closeDrawer(GravityCompat.START)
            }
            true
        }
        R.id.nav_chat -> {
            val intent = Intent(this, NewMessageActivity::class.java)
                startActivity(intent)
            true
        }
        R.id.nav_notification -> {
            setContentView(R.layout.activity_notification)
            not_close.setOnClickListener {
                val close = Intent(this@MessageActivity, MessageActivity::class.java)
                close.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP.or(Intent.FLAG_ACTIVITY_NO_HISTORY)
                startActivity(close)

            }

            true
        }
        R.id.nav_sign_out ->{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            startActivity(intent)
            true
        }
        R.id.nav_share -> {
            shareApp()
            true
        }
        R.id.nav_feedback -> {
        val sendEmail = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${getString(R.string.developer_mail)}")
        }
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject))
            startActivity(Intent.createChooser(sendEmail, getString(R.string.chooser_title)))
            true
        }
       else -> {
            false
        }
    }

    private var drawerLayout: DrawerLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         setContentView(R.layout.navigation_drawer)

        Log.d("device", "Device Name: ${getDeviceName()}, ver Name: ${BuildConfig.VERSION_NAME}, buildVer: ${Build.VERSION.RELEASE}")
        nav_device_name.text = getString(R.string.device, getDeviceName())
        nav_build_ver.text = getString(R.string.app_version, applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName)
        nav_os_name.text = getString(R.string.os, Build.VERSION.RELEASE)

        drawerLayout = findViewById(R.id.navigation_drawer_layout)
        val toolbar: Toolbar = findViewById(R.id.activity_main_toolbar)
        setSupportActionBar(toolbar)

        val navigationLayout: NavigationView = findViewById(R.id.nav_view)
        navigationLayout.setNavigationItemSelectedListener(this)
        val actionToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close)
        drawerLayout!!.setDrawerListener(actionToggle)
        actionToggle.syncState()

        recylerView_latestMessage.adapter = adapter
        recylerView_latestMessage.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))


        //set item click on listener adapter
        adapter.setOnItemClickListener { item, _ ->
            Log.d(TAG, "message 123")
            val intent = Intent(this, ChatLogActivity::class.java)
            //missing chat partner user
            val row = item as LatestMessageRow
            intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartnerUser)
            startActivity(intent)
        }

//        setUpDummyRows()
        listenForLatestMessage()
        fetchCurrentUser()
        verifyUserLoggedIn()
        mobileAd()
    }



    private fun mobileAd(){
        MobileAds.initialize(this, getString(R.string.Admob_appID))
        val adRequests = AdRequest.Builder().build()
        adView.loadAd(adRequests)
        adView2.loadAd(adRequests)
        adView3.loadAd(adRequests)
//        val adView = AdView(this)
//        adView.adSize = AdSize.SMART_BANNER
//        adView.adUnitId = R.string.Admob_bannerAd.toString()
        adView.visibility = View.GONE
        adView2.visibility = View.GONE
        adView3.visibility = View.GONE
        adView.adListener = object: AdListener(){
            override fun onAdOpened() {
                adView.visibility = View.VISIBLE
                adView.loadAd(adRequests)
                super.onAdOpened()
            }
            override fun onAdLoaded() {
                adView.visibility = View.VISIBLE
                Log.d("Ad", "adloaded")
                super.onAdLoaded()
            }
        }
        adView2.adListener = object: AdListener(){
            override fun onAdLoaded() {
                adView2.visibility = View.VISIBLE
                super.onAdLoaded()
            }
            override fun onAdClicked() {
                adView2.visibility = View.GONE
                super.onAdClicked()
            }
            override fun onAdOpened() {
                adView2.visibility = View.GONE
                super.onAdOpened()
            }
        }
        adView3.adListener = object : AdListener(){
            override fun onAdLoaded() {
                adView3.visibility = View.VISIBLE
                super.onAdLoaded()
            }

            override fun onAdOpened() {
                adView3.visibility = View.GONE
                super.onAdOpened()
            }
        }
        adView4.adListener = object : AdListener(){
            override fun onAdLoaded() {
                adView4.visibility = View.VISIBLE
                super.onAdLoaded()
            }

            override fun onAdClicked() {
                adView4.visibility = View.GONE
                super.onAdClicked()
            }
            override fun onAdOpened() {
                adView4.visibility = View.GONE
                super.onAdOpened()
            }
        }
        adView5.adListener = object : AdListener(){
            override fun onAdLoaded() {
                adView5.visibility = View.VISIBLE
                super.onAdLoaded()
            }
            override fun onAdClicked() {
                adView5.visibility = View.GONE
                super.onAdClicked()
            }
            override fun onAdOpened() {
                adView5.visibility = View.GONE
                super.onAdOpened()
            }
        }


    }

    val latestMessageMap = HashMap<String, ChatMessage>()
    private fun refreshRecycleViewMessage(){
        adapter.clear()
        latestMessageMap.values.forEach {
            adapter.add(LatestMessageRow(it))
        }
    }
    private fun listenForLatestMessage(){
        val fromID = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromID")
        ref.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)?: return

                latestMessageMap[p0.key!!] = chatMessage
                refreshRecycleViewMessage()
                //adapter.add(LatestMessageRow(chatMessage))
            }
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)?: return
                adapter.add(LatestMessageRow(chatMessage))
                latestMessageMap[p0.key!!] = chatMessage
                refreshRecycleViewMessage()

            }
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }
        })
    }
    val adapter = GroupAdapter<ViewHolder>()
  /*  private fun setUpDummyRows(){
        adapter.add(LatestMessageRow())
        adapter.add(LatestMessageRow())
        adapter.add(LatestMessageRow())

    } */
    private fun fetchCurrentUser(){
      val picasso = Picasso.get()
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                currentUser = p0.getValue(User::class.java)
                Log.d("Latest Message", "Current User: ${currentUser?.userName}")

               nav_user.text = getString(R.string.username, currentUser!!.userName) ?: "user"

                picasso.load(currentUser?.profileImageUrl)
                    .placeholder(R.drawable.ic_baseline_broken_image_24)
                    .resize(50, 50)
                    .centerCrop()
                    .error(R.drawable.ic_baseline_broken_image_24)
                    .into(findViewById<ImageView>(R.id.nav_image))
         }
            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun verifyUserLoggedIn(){

        val uid = FirebaseAuth.getInstance().uid
        if (uid == null){
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }



  private fun shareApp(){
      val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
      sharingIntent.type = "text/plain"
      val shareBody = "Application link: https://play.google.com/store/apps/details?id=com.sandeep.androidchat"
      sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "App Link")
      sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
      startActivity(Intent.createChooser(sharingIntent, "Share App Link Via:"))
  }



}
