package org.koitharu.kotatsu.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import androidx.core.graphics.get
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.core.model.MangaPage
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.utils.ext.await
import org.koitharu.kotatsu.utils.ext.medianOrNull
import org.koitharu.kotatsu.utils.ext.use
import java.io.File
import java.io.InputStream

object MangaUtils : KoinComponent {

	/**
	 * Automatic determine type of manga by page size
	 * @return ReaderMode.WEBTOON if page is wide
	 */
	suspend fun determineReaderMode(pages: List<MangaPage>): ReaderMode? {
		try {
			val page = pages.medianOrNull() ?: return null
			val url = MangaProviderFactory.create(page.source).getPageFullUrl(page)
			val client = get<OkHttpClient>()
			val request = Request.Builder()
				.url(url)
				.get()
				.build()
			val size = client.newCall(request).await().use {
				getBitmapSize(it.body?.byteStream())
			}
			return when {
				size.width * 2 < size.height -> ReaderMode.WEBTOON
				else -> ReaderMode.STANDARD
			}
		} catch (e: Exception) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
			return null
		}
	}

	@JvmStatic
	private fun getBitmapSize(input: InputStream?): Size {
		val options = BitmapFactory.Options().apply {
			inJustDecodeBounds = true
		}
		BitmapFactory.decodeStream(input, null, options)
		val imageHeight: Int = options.outHeight
		val imageWidth: Int = options.outWidth
		check(imageHeight > 0 && imageWidth > 0)
		return Size(imageWidth, imageHeight)
	}

	@JvmStatic
	fun cropBitmap(input: Bitmap): Bitmap? {
		return try {
			val bounds = Rect(0, 0, input.width, input.height)
			var isBoundsChanged = false
			for (x in 1 until input.width / 2) {
				var leftColor = 0
				var rightColor = 0
				for (y in 0 until input.height) {
					leftColor += input[x, y]
					rightColor += input[input.width - x - 1, y]
				}
				leftColor /= input.height
				rightColor /= input.height
				var consumed = false
				if (leftColor == Color.WHITE) {
					bounds.left++
					consumed = true
				}
				if (rightColor == Color.WHITE) {
					bounds.right--
					consumed = true
				}
				if (consumed) {
					isBoundsChanged = true
				} else {
					break
				}
			}
			for (y in 1 until input.height / 2) {
				var topColor = 0
				var bottomColor = 0
				for (x in 0 until input.width) {
					topColor += input[x, y]
					bottomColor += input[x, input.height - y - 1]
				}
				topColor /= input.width
				bottomColor /= input.width
				var consumed = false
				if (topColor == Color.WHITE) {
					bounds.top++
					consumed = true
				}
				if (bottomColor == Color.WHITE) {
					bounds.bottom--
					consumed = true
				}
				if (consumed) {
					isBoundsChanged = true
				} else {
					break
				}
			}
			if (isBoundsChanged) {
				Bitmap.createBitmap(input, bounds.left, bounds.top, bounds.width(), bounds.height())
			} else {
				null
			}
		} catch (e: Throwable) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace()
			}
			null
		}
	}

	@JvmStatic
	fun cropBitmap(file: File) {
		BitmapFactory.decodeFile(file.path).use { bmp ->
			cropBitmap(bmp)?.use { cropped ->
				file.outputStream().use { out ->
					cropped.compress(Bitmap.CompressFormat.WEBP, 100, out)
				}
			}
		}
	}
}