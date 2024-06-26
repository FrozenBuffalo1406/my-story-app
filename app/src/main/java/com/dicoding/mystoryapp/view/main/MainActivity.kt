package com.dicoding.mystoryapp.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.mystoryapp.R
import com.dicoding.mystoryapp.adapter.LoadingStateAdapter
import com.dicoding.mystoryapp.adapter.StoryAdapter
import com.dicoding.mystoryapp.factory.ViewModelFactory
import com.dicoding.mystoryapp.databinding.ActivityMainBinding
import com.dicoding.mystoryapp.view.upload.UploadActivity
import com.dicoding.mystoryapp.view.auth.AuthActivity
import com.dicoding.mystoryapp.view.detail.DetailActivity
import com.dicoding.mystoryapp.view.maps.MapsActivity
import com.dicoding.mystoryapp.view.upload.UploadViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private val uploadViewModel by viewModels<UploadViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var adapter: StoryAdapter
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.isLoading.observe(this) {isLoading ->
            showLoading(isLoading)
        }

        viewModel.getSession().observe(this) { user ->
            if (!user.isLogin) {
                startActivity(Intent(this, AuthActivity::class.java))
                uploadViewModel.fetchStories()
                finish()
            }
        }

        setupView()
        showStory()
        binding.fabAddStory.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }
        uploadViewModel.isLoading.observe(this){
            showLoading(it)
        }

    }

    override fun onResume() {
        super.onResume()
        uploadViewModel.fetchStories()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out -> {
                viewModel.signout()
                true
            }
            R.id.btn_maps-> {
                startActivity(Intent(this@MainActivity, MapsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoading(isLoading: Boolean){
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showStory() {
        binding.rvStory.layoutManager = LinearLayoutManager(this)
        adapter = StoryAdapter{ story ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_STORY, story)
            startActivity(intent)

        }
        binding.rvStory.adapter = adapter.withLoadStateFooter(
            footer = LoadingStateAdapter {
                adapter.retry()
            }
        )
        adapter.addLoadStateListener { loadState ->
            if (loadState.refresh is LoadState.Loading) {
                showLoading(true)
            } else {
                showLoading(false)
            }
        }
        uploadViewModel.storiess.observe(this) {
            adapter.submitData(lifecycle,it)
        }
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }


}