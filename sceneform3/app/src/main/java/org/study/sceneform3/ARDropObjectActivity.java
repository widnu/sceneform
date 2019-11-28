package org.study.sceneform3;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

public class ARDropObjectActivity extends AppCompatActivity {
    private ArFragment fragment;
    private PointerDrawable pointer = new PointerDrawable();
    private boolean isTracking;
    private boolean isHitting;
    private ModelLoader modelLoader;
    ArFragment arFragment;
    boolean shouldAddModel = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ardropobject);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        arAndroidOnTheFloor();

    }

    private void arAndroidOnTheFloor() {
        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate();
        });
        modelLoader = new ModelLoader(new WeakReference<>(this));
        initializeGallery();
    }

    private void arOnReferenceImage() {
        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {/* Inflate the menu; this adds items to the action bar if it is present.*/
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {/* Handle action bar item clicks here. The action bar will automatically handle clicks on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.*/
        int id = item.getItemId();/*noinspection SimplifiableIfStatement*/
        if (id == R.id.action_settings) return true;
        return super.onOptionsItemSelected(item);
    }

    private void onUpdate() {
        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) contentView.getOverlay().add(pointer);
            else contentView.getOverlay().remove(pointer);
            contentView.invalidate();
        }
        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }

    private boolean updateTracking() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth() / 2, vw.getHeight() / 2);
    }

    private void initializeGallery() {
        LinearLayout gallery = findViewById(R.id.gallery_layout);
        ImageView andy = new ImageView(this);
        andy.setImageResource(R.drawable.droid_thumb);
        andy.setContentDescription("andy");
        andy.setOnClickListener(view -> {
            addObject(Uri.parse("andy_dance.sfb"));
        });
        gallery.addView(andy);/*        ImageView cabin = new ImageView(this); cabin.setImageResource(R.drawable.cabin_thumb); cabin.setContentDescription("cabin"); cabin.setOnClickListener(view ->{addObject(Uri.parse("Cabin.sfb"));}); gallery.addView(cabin);*//*        ImageView house = new ImageView(this); house.setImageResource(R.drawable.house_thumb); house.setContentDescription("house"); house.setOnClickListener(view ->{addObject(Uri.parse("House.sfb"));}); gallery.addView(house);*//*        ImageView igloo = new ImageView(this); igloo.setImageResource(R.drawable.igloo_thumb); igloo.setContentDescription("igloo"); igloo.setOnClickListener(view ->{addObject(Uri.parse("igloo.sfb"));}); gallery.addView(igloo);*/
    }

    private void addObject(Uri model) {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    modelLoader.loadModel(hit.createAnchor(), model);
                    break;
                }
            }
        }
    }

    public void addNodeToScene(Anchor anchor, ModelRenderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();/*        startAnimation(node, renderable);*/
    }

    public void onException(Throwable throwable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(throwable.getMessage()).setTitle("Codelab error!");
        AlertDialog dialog = builder.create();
        dialog.show();
        return;
    }/*    public void startAnimation(TransformableNode node, ModelRenderable renderable){ if(renderable==null || renderable.getAnimationDataCount() == 0) { return; } for(int i = 0;i < renderable.getAnimationDataCount();i++){ AnimationData animationData = renderable.getAnimationData(i); } ModelAnimator animator = new ModelAnimator(renderable.getAnimationData(0), renderable); animator.start(); node.setOnTapListener( (hitTestResult, motionEvent) -> { togglePauseAndResume(animator); }); } public void togglePauseAndResume(ModelAnimator animator) { if (animator.isPaused()) { animator.resume(); } else if (animator.isStarted()) { animator.pause(); } else { animator.start(); } }*/

    public boolean setupAugmentedImagesDb(Config config, Session session) {
        AugmentedImageDatabase augmentedImageDatabase;
        Bitmap bitmap = loadAugmentedImage();
        if (bitmap == null) {
            return false;
        }
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("world", bitmap);
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Bitmap loadAugmentedImage() {
        try (InputStream is = getAssets().open("world.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("ImageLoad", "IO Exception", e);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : augmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                if (augmentedImage.getName().equals("world") && shouldAddModel) {
                    placeObject(arFragment, augmentedImage.createAnchor(augmentedImage.getCenterPose()), Uri.parse("Mesh_BengalTiger.sfb"));
                    shouldAddModel = false;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void placeObject(ArFragment arFragment, Anchor anchor, Uri uri) {
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> addNodeToScene(arFragment, anchor, modelRenderable))
                .exceptionally(throwable -> {
                    Toast.makeText(arFragment.getContext(), "Error:" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    private void addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }
}
