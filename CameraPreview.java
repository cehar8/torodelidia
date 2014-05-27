package ioio.examples.hello;

import java.io.IOException;
import java.lang.Thread;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


	/** A basic Camera preview class */
	public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	    private SurfaceHolder mHolder;
	    private Camera mCamera;
	    private static final String TAG = "MainActivity";

	    public CameraPreview(Context context, Camera camera) {
	        super(context);
	        mCamera = camera;

	        // Install a SurfaceHolder.Callback so we get notified when the
	        // underlying surface is created and destroyed.
	        mHolder = getHolder();
	        mHolder.addCallback(this);
	        // deprecated setting, but required on Android versions prior to 3.0
	        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    }

	    public void surfaceCreated(SurfaceHolder holder) {
	        // The Surface has been created, now tell the camera where to draw the preview.
	        try {
	            mCamera.setPreviewDisplay(holder);
	            
	            mCamera.startPreview();
	        } catch (IOException e) {
	            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
	        }
	    }

	    public void surfaceDestroyed(SurfaceHolder holder) {
	    	try {
	            mCamera.stopPreview();
	            mCamera.release();
	        } catch (Exception e){
	          // ignore: tried to stop a non-existent preview
	        }
	    }

	    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
	        // If your preview can change or rotate, take care of those events here.
	        // Make sure to stop the preview before resizing or reformatting it.

	        if (mHolder.getSurface() == null){
	          // preview surface does not exist
	          return;
	        }

	        // stop preview before making changes
	        try {
	            mCamera.stopPreview();
	        } catch (Exception e){
	          // ignore: tried to stop a non-existent preview
	        }

	        // set preview size and make any resize, rotate or
	        // reformatting changes here

	        Camera.Parameters params = mCamera.getParameters();
	        Camera.Size size = getBestPreviewSize(params, w, h);
	        params.setPreviewSize(size.width, size.height);
	        params.setFlashMode(Parameters.FLASH_MODE_TORCH);
	        //params.setPreviewFrameRate(1);
	        mCamera.setParameters(params);
	        params.setPreviewFormat(ImageFormat.NV21);

	        Thread preview_thread = new Thread(new Runnable() {
	            @Override
	            public void run() {
	                   mCamera.startPreview();
	               }
	            }, "preview_thread");
	            preview_thread.start(); 
	    }
	    
	    public Camera.Size getBestPreviewSize(Camera.Parameters parameters, int w, int h)
	    {
	        Camera.Size result = null; 

	        for (Camera.Size size : parameters.getSupportedPreviewSizes())
	        {
	            if (size.width <= w && size.height <= h)
	            {
	                if (null == result)
	                result = size; 
	            else
	            {
	                int resultDelta = w - result.width + h - result.height;
	                int newDelta    = w - size.width   + h - size.height;

	                    if (newDelta < resultDelta)
	                result = size; 
	            }
	            } 
	        }
	        return result; 
	    }
	    
	}
	


