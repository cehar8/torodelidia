package ioio.examples.hello;

import java.io.IOException;
import java.lang.Number;
import java.lang.Math.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.hardware.Camera;


public class MainActivity extends IOIOActivity {
	
	private Camera myCam;
	private CameraPreview myPrev;
	//specs of the camera
	public static int widtha = 480;
	public static int heighta = 480;
	//lock to make Preview Callback critical code
	private ReentrantLock l;
	//integer that communicates between Preview Callback and IOIO loop
	public AtomicInteger is_red_drive = new AtomicInteger(0);
	int set = 0;
	double red1 = 400;
	double red2 = 400;
	double red3 = 400;
	double red4 = 400;
	
	//code to initialise OpenCV toolbox
	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
		   switch (status) {
		       case LoaderCallbackInterface.SUCCESS:
		       {
		    	   System.out.println("OpenCV loaded");
		       } break;
		       default:
		       {
		      super.onManagerConnected(status);
		       } break;
		   }
		    }
		};
	
	//safe camera open
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    return c; // returns null if camera is unavailable
	}
	
	//Image processing thread
	private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback()  
	  {
	  	
	          public void onPreviewFrame(byte[] data, Camera camera)  
	          {
	        	  l = new ReentrantLock();
	        	  //lock the code - no other code can interrupt
	        	  l.lock();
	      	  
	        	  //declare matrices
	        	  final Mat red_only;
	      		  final Mat mYuv;
	      		  final Mat mRgb;
	      		  
	      		  //variables for direction - indicate greater than 800 pixels found in third
	      		  boolean left = false;
	      		  boolean right = false;
	      		  boolean mid = false;
	      		  
	      		  Scalar min;
	      		  Scalar max;
	      				
	      		  
	      		
	      		  //initialising matrices, CvType.CV_8UC1 - indicates a matrix with 3 channels per entry (for R, G and B values)
	      		  red_only = new Mat( heighta, widtha, CvType.CV_8UC1);
	        	  mYuv = new Mat(heighta, widtha, CvType.CV_8UC1);
	        	  mRgb = new Mat(heighta, widtha, CvType.CV_8UC1);
	        	  
	        	  //put data from Camera Preview into a Yuv matrix
	        	  mYuv.put( 0, 0, data );
	        	  
	        	  //convert Yuv data to TGB
                  Imgproc.cvtColor( mYuv, mRgb, Imgproc.COLOR_YUV420sp2RGB, 0);
                  if (set==10) {
                	  set = 101;
                	  red1 = mRgb.get(240,240)[0];
                	  red2 = mRgb.get(240,240)[1];
                	  red3 = mRgb.get(240,240)[2];
                	  System.out.println(red1);
                	  System.out.println(red2);
                	  System.out.println(red3);
                  }
                  else {
                	  set++;
                  }
                  
                  min = new Scalar(red1-40, red2-20, red3-20);
                  max = new Scalar(255, red2+20, red3+20);
                  
                  //set what RGB range will be defined as red
                  
                  //check if any entries from the RGB matrix fall within the red definition
                  Core.inRange(mRgb, min, max, red_only);
                  //convert the Mat into an array (3d to 2d)
                  Core.extractChannel(red_only, red_only, 0);
                  
                  //split the 2d matrix into 3 parts
                  Mat red_left = new Mat(heighta, widtha/3, CvType.CV_8UC1);                  
                  Mat red_mid = new Mat(heighta, widtha/3, CvType.CV_8UC1);
                  Mat red_right = new Mat(heighta, widtha/3, CvType.CV_8UC1);
                  red_right = red_only.submat(0, red_only.rows()/3, 0, red_only.cols());
                  red_mid = red_only.submat(red_only.rows()/3, 2*red_only.rows()/3, 0, red_only.cols());
                  red_left = red_only.submat(2*red_only.rows()/3, red_only.rows(), 0, red_only.cols());
                  
                  //count number of non-zero entries in each counter
                  int red_counter = Core.countNonZero(red_only);
                  System.out.println(red_counter);
                  int left_counter = Core.countNonZero(red_left);
                  int mid_counter = Core.countNonZero(red_mid);
                  int right_counter = Core.countNonZero(red_right);
                  
                  //set boolean variables based on which matrix thirds meet the threshold of 800 pixels
                  if (left_counter>2000) {
                	 left = true;
                  }
                  if (mid_counter>2000) {
                	  mid = true;
                  }
                  if (right_counter>2000) {
                	  right = true;
                  }
                  
                  //only test if one of the thresholds has been hit
                  if(left|mid|right) {
                  
                	  //if all thresholds - go straight
                  if (left&mid&right) {
                	  System.out.println("STRAIGHT");
                	  is_red_drive.set(2);
                  }
                  //if left and mid - go left
                  if (left&mid&(!right)) {
                	  System.out.println("LEFT"); 
                	  is_red_drive.set(1);
                  }
                  //if left only - go left
                  if (left&(!mid)&(!right)) {
                	  System.out.println("LEFT");
                	  is_red_drive.set(1);
                  }
                  //if mid and right - go right
                  if ((!left)&mid&right) {
                	  System.out.println("RIGHT");
                	  is_red_drive.set(3);
                  }
                  //if right only - go right
                  if ((!left)&(!mid)&right) {
                	  System.out.println("RIGHT");
                	  is_red_drive.set(3);
                  }
                  //if mid only - go straight
                  if ((!left)&mid&(!right)) {
                	  System.out.println("STRAIGHT");
                	  is_red_drive.set(2);
                  }
                  //if left and right but no mide - stop
                  if ((left)&(!mid)&(right)) {
                	  System.out.println("STOP");
                	  is_red_drive.set(0);
                  }
                  }
                  else {
                	  is_red_drive.set(0);
                  }

                  //end of critical code
                  l.unlock();
            } 
	  };
	  
	//initialisaiton section  
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		//activate OpenCV
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack);
		//set layout - camera view
		setContentView(R.layout.main);
		
		//remove the callback if myPrev wasn't successfully created
        if(myPrev!=null) {
    		myPrev.getHolder().removeCallback(myPrev);
    	}
        
        // Create an instance of Camera
        myCam = getCameraInstance();
        
        // create the preview view
        myPrev = new CameraPreview(this, myCam);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(myPrev);
        
        //create the previewCallback (image processing method)
        myCam.setPreviewCallback(previewCallback);
	}

	//initialise the IOIO pins
	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */

		private DigitalOutput led_;
		private DigitalOutput pwmB;
		private DigitalOutput pwmA;
		private DigitalOutput Binone;
		private DigitalOutput Bintwo;
		private DigitalOutput Ainone;
		private DigitalOutput Aintwo;
		private DigitalOutput standby;
		private DigitalOutput gnd;

		
		@Override
		protected void setup() throws ConnectionLostException {
			
			//debugging led - will turn on when robot see red even if motors aren't working
			led_ = ioio_.openDigitalOutput(0, true);
			
			//initialising pins at the narrow end of the IOIO board
			pwmA = ioio_.openDigitalOutput(20);
			pwmB = ioio_.openDigitalOutput(26);
			Binone = ioio_.openDigitalOutput(24);
			Bintwo = ioio_.openDigitalOutput(25);
			Ainone = ioio_.openDigitalOutput(22);
			Aintwo = ioio_.openDigitalOutput(21);
			standby = ioio_.openDigitalOutput(23);
			gnd = ioio_.openDigitalOutput(27);
			
			//giving the pins initial values
			pwmA.write(true);
			pwmB.write(true);
			Binone.write(false);
			Bintwo.write(false);
			Ainone.write(false);
			Aintwo.write(false);
			standby.write(true);
			gnd.write(false);	
		}

		//constantly polls and updates the IOIO confirming conection and changing pin values
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			//System.out.println("IN LOOOOOOOOOOOOOOOOOP");
			//code to run motors forward
			if (is_red_drive.get()==2){
			led_.write(false);		
			Binone.write(false);
			Bintwo.write(true);
			Ainone.write(true);
			Aintwo.write(false);
			//System.out.println("RED FOUND");
			}
			//code to stop motors
			if (is_red_drive.get()==0) {
				led_.write(true);
				Binone.write(false);
				Bintwo.write(false);
				Ainone.write(false);
				Aintwo.write(false);
				
			}
			//code to turn robot left
			if (is_red_drive.get()==3) {
				Binone.write(false);
				Bintwo.write(false);
				Ainone.write(true);
				Aintwo.write(false);
			}
			//code to turn robot right
			if (is_red_drive.get()==1) {
				Binone.write(false);
				Bintwo.write(true);
				Ainone.write(false);
				Aintwo.write(false);
			}
			//Thread.sleep(100);
		}
			
	}
	
	//method that initialises the IOIO thread
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}
