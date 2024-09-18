# What is SLR Core?

SLR Core is a unified set of interfaces that define how our the ML Models for our Sign Language Recognition will communicate with each other, the app that the user is developing, and the platform on which the user is developing.
This current documentation is specifically focused on Kotlin development for the Android Platform.

SLR Core has many fundamental abstractions, that are very low level. 
A developer can simply be aware of the `Simple Execution Engine` and have the entire SLR Core functionality working for their application - this documentation shows how to use this engine.
However, if this execution engine is not optimized enough or flexible enough for the Developer's use case, they might choose to use the components of Core and construct a different execution engine or pipeline for their use case. This is beyond the scope of this documentation.

SLR Core is a definition of interfaces that are then implemented for each platform individually. This framework is still under development and will have a lot of changes before it is production ready. However it is functional enough to start building apps with - specifically proof of concept apps. The goal is to have the SLR Core interface unified across each platform, and abstract away plaform specific requirements for running common tasks such as Mediapipe, Tensorflow Lite, or camera interfaces/ canvas interfaces.

SLR Core currently has the following components:
    - Camera: This specifies an interface to interact with the platform's camera. This interface realizes that the camera just needs to produce frames that can be pushed into the SLR pipeline.
    - Model: This specifies an interface to use Mediapipe and Tflite, and also adds features such as the networked model manager, which can autoupdate from our central server.
    - Preview: This specifies an interface to work with traditional canvas based UI painting. This interface realizes that SLR activity (for the time being this just includes mediapipe hand recognition) can be easily visualized once 3 different painting instructions are defined - how to paint a point, how to paint a line and how to paint an image. 
    - System: This specifies basic system components for the framework - such as how the mediapipe hand landmarks are buffered before being sent to the SLR model, since the model requires 60 landmarks currently to make a prediction. 
    It also allows for fine grained control, by allowing the user to swap out the buffer trigger/flush condition and the callbacks that are triggered when the buffer is flushed.
    - Engine: Currently only the Simple Execution Engine is defined. This engine connects the android camera using camerax, to mediapipe, then buffers those outputs which are fed into the SLR model. 
    - Common: This contains some common utility functions and types.

# Getting Started with the SLRCore Library
1. Download and store the SLRCore aar in a known directory on your system. Ensure that the MediaPipe model, `hand-landmarker.task`, is also in the `res/assets` folder of your project.  
2.  In your build.gradle.kts for your app (not project), you need to add the dependency:
```
implementation(files("../slrcore-release.aar"))
```
3. SLRCore will require further dependencies to be added:
Camera Dependencies:
```
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-video:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
implementation("androidx.camera:camera-extensions:1.3.1")
```

Mediapipe Dependencies:
```
implementation("com.google.mediapipe:tasks-vision:0.20230731")
```

TensorFlow-Lite Dependencies:
```
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("org.tensorflow:tensorflow-lite-gpu-api:2.12.0")
implementation("org.tensorflow:tensorflow-lite-gpu:2.15.0")
implementation("org.tensorflow:tensorflow-lite:2.13.0")
```

Additional Dependencies for using the networked model manager feature (auto-update the model from our central server):
```
implementation("org.eclipse.angus:jakarta.mail:2.0.3")
```

Also, add this to the under `android {}`, to allow for a clean build since some JAR files in the above dependencies will have files that android cannot place into apks: 
```
packaging { 
    resources { 
        excludes += "META-INF/DEPENDENCIES" 
        excludes += "META-INF/LICENSE" 
        excludes += "META-INF/LICENSE.txt" 
        excludes += "META-INF/license.txt" 
        excludes += "META-INF/NOTICE" 
        excludes += "META-INF/NOTICE.txt" 
        excludes += "META-INF/notice.txt" 
        excludes += "META-INF/ASL2.0" 
        excludes += "META-INF/*.kotlin_module" 
        excludes += "META-INF/NOTICE.md" 
        excludes += "META-INF/LICENSE.md" 
    } 
}
```

4. The simplest way to use core would be to use the `SimpleExecutionEngine` that has all the different components set up out of the box. For a more complex use case, you will have to setup your own pipeline or execution engine with the necessary callback mechanisms synced as SimpleExecutionEngine has done. Since this is a basic tutorial, we will have a global variable in our `MainActivity.kt` file: `lateinit var SLREngine: SimpleExecutionEngine`

5. We now need to request the permissions that this app will require. This will mean that in the manifest file we have:
```
<uses-feature android:name="android.hardware.camera.any" />
<uses-permission android:name="android.permission.CAMERA" />
```
Then, in the `MainActivity.kt` file, we add:
```
val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
PERMISSIONS_REQUIRED.forEach {
    if (ContextCompat.checkSelfPermission(
            baseContext,
            it
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted)
                Toast.makeText(this, "$it permission ", Toast.LENGTH_SHORT).show()
        }.launch(it)
    }
}
```

6. We can now initialise our engine: 
```
	SLREngine = SimpleExecutionEngine(this, {
            this.signPredictor.outputFilters.add(Thresholder(0.8F))
        }) { sign ->
            runOnUiThread {
                Toast.makeText(this, "Guessed: ${sign} ", Toast.LENGTH_SHORT).show()
            }
        }
        SLREngine.buffer.trigger = NoTrigger()
```
The constructor takes in 2 lambdas. The first is a lambda that happens once the engine has finished initializing (specifically in the context of the fact that the engine uses a networked model, so it will call this callback after it has updated to the latest sign predictor). What we are doing is just adding a simple threshold filter that will only allow a sign to be predicted if the model is 80% confident in its prediction.

We also have a callback on each sign prediction, where we just display the sign predicted as an android toast.

In our example, we don't want to trigger the rest of the pipeline in our engine, so we have made the buffer trigger a NoTrigger so that the buffer does not trigger the rest of the pipeline unless we manually tell it to.

7. To add a preview feature, we are going to do the following: 

7a. Have an instance variable to keep track of the current prediction result. 
```
private val currResult = MutableStateFlow (
    ImageMPResultWrapper(Empties.EMPTY_HANDMARKER_RESULTS, Empties.EMPTY_BITMAP)
)
```

7b. Add a callback to the pose predictor to update the current result. 
```
SLREngine.posePredictor.addCallback("preview_update") {
    mpResult -> currResult.value = mpResult
}
```

8. When we start working with compose, we will want to have a basic UI state that will be able to indicate when to send the camera images to the SLREngine, along with some UI state to be able to keep track of the current result.
We can use compose to do this trivially in our Composable function:
```
val mpResult by this.currResult.collectAsState()
val interaction = remember { MutableInteractionSource() }
val isCameraVisible by interaction.collectIsPressedAsState()
```
8a. We want the engine to collect images when the camera is visible, and once the camera is not visible, we want it to trigger the rest of the pipeline. This way, we can form some basic interaction where the user taps on the screen to start the camera, the engine collects images from the camera till the user taps the screen again, in which case our buffer triggers. Since our buffer has by default a capacity of 60 images, this will mean that we only look at the last 60 images once that pipeline is triggered.
```
if (isCameraVisible) {
    SLREngine.poll()
} else {
    if (SLREngine.buffer.trigger is NoTrigger)
        SLREngine.buffer.triggerCallbacks()
    SLREngine.pause()
    // inputProgress.value = 0
} 
```

8b. We now define a parent Compose Scaffold where our app will reside, and look at the User Interactions inside this scaffold to trigger the camera - when the user taps once, the camera is visible, if the user taps again, the camera is not visible and so on:
```
Scaffold (
    modifier = Modifier
        .fillMaxSize(),
    floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = {},
            interactionSource = interaction
        ) {
            Icon(Icons.Sharp.ThumbUp, "Sign Language Input")
        }
    },
    floatingActionButtonPosition = FabPosition.Center
){ ... }
```

8c. Our scaffold now has to have a box with 2 elements. The canvas element in the background is going to paint the current image and the box in the front is going to have our app content. We can use a HandPreviewPainter, that is specifically designed to paint the hand landmark results from mediapipe. This painter requires a definition of how to paint some basic concepts - a point, a line and an image - using which it can paint the preview. These definitions can be defined by implementing the PainterI interface, however, we have a solution that works for compose elements really well called the ComposePainterInterface, that just requires the compose object onto which we will paint our preview, and in this case that is our box. We also tell the preview painter to paint the IMAGE and the SKELETON, although you can configure it to paint just one of the 2 as well. Finally we pass in the image and the hand landmarks, with the image width and the height, to this painter. The main part out here is the image preprocessing. 
```
{
        padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (isCameraVisible) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize(),
    ){
                onDraw = {
                    if (mpResult.image != Empties.EMPTY_BITMAP) {
                        val resultBmp = BitmapExtractor.extract(mpResult.image)
                        val targetAR = maxOf(size.width / resultBmp.width, size.height / resultBmp.height)
                        val img = Bitmap.createBitmap(
                            resultBmp,
                            0,
                            0,
                            resultBmp.width,
                            resultBmp.height,
                            Matrix().also {
                                it.setRectToRect(
                                    RectF(
                                        0f,
                                        0f,
                                        resultBmp.width.toFloat(),
                                        resultBmp.height.toFloat()
                                    ),
                                    RectF(
                                        0f,
                                        0f,
                                        resultBmp.width * targetAR,
                                        resultBmp.height * targetAR
                                    ),
                                    Matrix.ScaleToFit.FILL
                                )
                            },
                            true
                        )
                        HandPreviewPainter(
                            ComposeCanvasPainterInterface(
                                this
                            ),
                            PainterMode.IMAGE_AND_SKELETON
                        ).paint(
                            img,
                            mpResult.result,
                            img.width.toFloat(),
                            img.height.toFloat()
                        )
                    }
                }
            }
        }
        Content()
    }
}
```

9. Once you define your Content composable function, your app is good to go!
