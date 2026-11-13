package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.CarSpot
import java.io.File

/**
 * Returns the best high-resolution car drawable based on the make/model of the car
 */
fun getCarDrawableForSpot(car: CarSpot): Int {
    val make = car.make.lowercase()
    val model = car.model.lowercase()

    return when {
        make.contains("porsche") || model.contains("911") || model.contains("gt3") || model.contains("cayman") -> R.drawable.car_porsche
        make.contains("ferrari") || model.contains("296") || model.contains("f8") || model.contains("sf90") || model.contains("488") -> R.drawable.car_ferrari
        make.contains("lamborghini") || model.contains("revuelto") || model.contains("huracan") || model.contains("aventador") || model.contains("urus") -> R.drawable.car_lamborghini
        make.contains("bugatti") || model.contains("chiron") || model.contains("veyron") || model.contains("tourbillon") -> R.drawable.car_bugatti
        make.contains("bmw") || model.contains("m4") || model.contains("m3") || model.contains("m5") || model.contains("m2") || model.contains("csl") -> R.drawable.car_bmw
        make.contains("nissan") || model.contains("gt-r") || model.contains("gtr") || model.contains("nismo") || model.contains("z") -> R.drawable.car_nismo
        make.contains("mclaren") || model.contains("750s") || model.contains("720s") || model.contains("p1") || model.contains("artura") -> R.drawable.car_mclaren
        make.contains("corvette") || make.contains("chevrolet") || model.contains("corvette") || model.contains("z06") || model.contains("stingray") || model.contains("camaro") -> R.drawable.car_corvette
        make.contains("ford") || model.contains("mustang") || model.contains("dark horse") || model.contains("shelby") || model.contains("gt") -> R.drawable.car_mustang
        make.contains("audi") || model.contains("r8") || model.contains("rs") || model.contains("rs6") || model.contains("rs7") -> R.drawable.car_audi
        make.contains("mercedes") || make.contains("amg") || model.contains("amg") || model.contains("sl63") -> R.drawable.car_mercedes
        make.contains("toyota") || model.contains("supra") || model.contains("gr86") || model.contains("gr") -> R.drawable.car_toyota
        make.contains("dodge") || model.contains("viper") || model.contains("hellcat") -> R.drawable.car_mustang
        make.contains("aston") || model.contains("vantage") || model.contains("dbs") -> R.drawable.car_mclaren
        else -> R.drawable.car_porsche
    }
}

/**
 * High-performance, crisp vehicle display component:
 * Loads the user's authentic camera capture or the corresponding high-res vehicle photograph.
 */
@Composable
fun CarSpotImage(
    car: CarSpot,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    bitmap: Bitmap? = null
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "${car.make} ${car.model}",
            modifier = modifier,
            contentScale = contentScale
        )
        return
    }

    val context = LocalContext.current
    val photoUri = car.photoUri
    val fallbackDrawableRes = getCarDrawableForSpot(car)

    when {
        // Explicit pre-bundled resource references
        photoUri == "res:car_porsche" -> Image(painter = painterResource(id = R.drawable.car_porsche), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_ferrari" -> Image(painter = painterResource(id = R.drawable.car_ferrari), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_lamborghini" -> Image(painter = painterResource(id = R.drawable.car_lamborghini), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_bugatti" -> Image(painter = painterResource(id = R.drawable.car_bugatti), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_bmw" -> Image(painter = painterResource(id = R.drawable.car_bmw), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_nismo" -> Image(painter = painterResource(id = R.drawable.car_nismo), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_mclaren" -> Image(painter = painterResource(id = R.drawable.car_mclaren), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_corvette" -> Image(painter = painterResource(id = R.drawable.car_corvette), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_mustang" -> Image(painter = painterResource(id = R.drawable.car_mustang), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_audi" -> Image(painter = painterResource(id = R.drawable.car_audi), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_mercedes" -> Image(painter = painterResource(id = R.drawable.car_mercedes), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)
        photoUri == "res:car_toyota" -> Image(painter = painterResource(id = R.drawable.car_toyota), contentDescription = "${car.make} ${car.model}", modifier = modifier, contentScale = contentScale)

        // Real camera captured photo on device
        !photoUri.isNullOrBlank() && (photoUri.startsWith("file:") || photoUri.startsWith("/") || photoUri.startsWith("content:")) -> {
            val file = if (photoUri.startsWith("file://")) {
                File(Uri.parse(photoUri).path ?: "")
            } else if (photoUri.startsWith("/")) {
                File(photoUri)
            } else {
                null
            }

            val imageSource: Any = if (file != null && file.exists()) {
                file
            } else {
                Uri.parse(photoUri)
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(true)
                    .error(fallbackDrawableRes)
                    .placeholder(fallbackDrawableRes)
                    .build(),
                contentDescription = "${car.make} ${car.model}",
                modifier = modifier,
                contentScale = contentScale
            )
        }

        // High-res photographic automotive fallback
        else -> {
            Image(
                painter = painterResource(id = fallbackDrawableRes),
                contentDescription = "${car.make} ${car.model}",
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}
