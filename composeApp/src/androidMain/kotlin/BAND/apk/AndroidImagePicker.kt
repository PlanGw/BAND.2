package BAND.apk

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

class AndroidImagePicker(private val activity: ComponentActivity) : ImagePicker {
    private var onImagePickedCallback: ((String?) -> Unit)? = null

    private val pickMedia: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            onImagePickedCallback?.invoke(uri?.toString())
        }

    override fun pickImage(onImagePicked: (String?) -> Unit) {
        onImagePickedCallback = onImagePicked
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
