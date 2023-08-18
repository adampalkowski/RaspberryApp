package com.palrasp.raspberryapp

class ImageBuffer {
    private val imageUrls = mutableListOf<String>()

    fun addImageUrl(url: String) {
        imageUrls.add(url)
    }

    fun getNextImageUrl(): String? {
        if (imageUrls.isEmpty()) return null
        val nextImageUrl = imageUrls.first()
        imageUrls.removeAt(0)
        return nextImageUrl
    }
}
