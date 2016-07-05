package faceRecognition;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.FaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;


import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;




/**
 * The controller associated with the only view of our application. The
 * application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative
 * controls and the face detection/tracking.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 1.1 (2015-11-10)
 * @since 1.0 (2014-01-10)
 * 		
 */
public class FXController
{
	// FXML buttons
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	// checkboxes for enabling/disabling a classifier
	@FXML
	private CheckBox haarClassifier;
	@FXML
	private CheckBox lbpClassifier;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	
	// face cascade classifier
	private CascadeClassifier faceCascade;
	private int absoluteFaceSize;
	
	public int index = 316;
	public int ind = 0;
	
	// Names of the people from the training set
	public HashMap<Integer, String> names = new HashMap<Integer, String>();
	
	
	/**
	 * Init the controller, at start time
	 */
	protected void init()
	{
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.absoluteFaceSize = 0;
		// Takes some time thus use only when training set
		// was updated 
		trainModel();
	}
	


	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera()
	{
		// set a fixed width for the frame
		originalFrame.setFitWidth(600);
		// preserve image ratio
		originalFrame.setPreserveRatio(true);
		
		if (!this.cameraActive)
		{
			// disable setting checkboxes
			this.haarClassifier.setDisable(true);
			this.lbpClassifier.setDisable(true);
			
			// start the video capture
			this.capture.open(0);
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						Image imageToShow = grabFrame();
						originalFrame.setImage(imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				this.cameraButton.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");
			// enable classifiers checkboxes
			this.haarClassifier.setDisable(false);
			this.lbpClassifier.setDisable(false);
			
			// stop the timer
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			
			// release the camera
			this.capture.release();
			// clean the frame
			this.originalFrame.setImage(null);
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame()
	{
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					// face detection
					this.detectAndDisplay(frame);
					
					// convert the Mat object (OpenCV) to Image (JavaFX)
					imageToShow = mat2Image(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.println("ERROR: " + e);
			}
		}
		
		return imageToShow;
	}
	
	
	private void trainModel () {
		// Read the data from the training set
				File root = new File("resources/trainingset/combined/");
									
				
				FilenameFilter imgFilter = new FilenameFilter() {
		            public boolean accept(File dir, String name) {
		                name = name.toLowerCase();
		                return name.endsWith(".png");
		            }
		        };
		        
		        File[] imageFiles = root.listFiles(imgFilter);
		        
		        List<Mat> images = new ArrayList<Mat>();
		        
		        System.out.println("THE NUMBER OF IMAGES READ IS: " + imageFiles.length);
		        
		        
//		        Mat labels = new Mat(imageFiles.length, 1, CvType.CV_32SC1);
//		        List<Integer> trainingLabels = new ArrayList<>();
		        
		        // test
		        // Mat trainingImages = new Mat(0,250*250,CvType.CV_32SC1);
		        Mat labels = new Mat(imageFiles.length,1,CvType.CV_32SC1);
//		        List<Integer> trainingLabels = new ArrayList<>();
		        
		        int counter = 0;
		        
		        for (File image : imageFiles) {
		        	// Parse the training set folder files
		        	Mat img = Imgcodecs.imread(image.getAbsolutePath());
		        	// Change to Grayscale and equalize the histogram
		        	Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
		        	Imgproc.equalizeHist(img, img);
		        	// Extract label from the file name
		        	int label = Integer.parseInt(image.getName().split("\\-")[0]);
		        	// Extract name from the file name and add it to names HashMap
		        	String labnname = image.getName().split("\\_")[0];
		        	String name = labnname.split("\\-")[1];
		        	names.put(label, name);
		        	// debug
		        	//System.out.println("NAME IS: " + name);
		        	//System.out.println("Photo: " + counter + " Label: " + label + " Path: " + image.getAbsolutePath());
		        	// Add training set images to images Mat
		        	images.add(img);
		        	
		        	// Imgcodecs.imwrite("IMG-to-train-on" + counter + ".png", img);
		        	
//		        	trainingImages.push_back(img);
//		        	trainingLabels.add(label);
//		        	labels[counter] = label;
		        	labels.put(counter, 0, label);
		        	counter++;
		        }
		        
//		        Integer[] array = trainingLabels.toArray(new Integer[trainingLabels.size()]);
		        
		        	FaceRecognizer faceRecognizer = Face.createFisherFaceRecognizer(0,1500);
		        	faceRecognizer.train(images, labels);
		        	faceRecognizer.save("traineddata");
	}
	
	/** Method for face recognition
	 * 
	 * grabs the detected face and matches it with
	 * the training set. If recognized the name of
	 * the person is printed below the face rectangle
	 * @return 
	 */
	
	private double[] faceRecognition(Mat currentFace) {

//		Imgproc.cvtColor(currentFace, currentFace, Imgproc.COLOR_BGR2GRAY);
//		Imgproc.equalizeHist(currentFace, currentFace);
		
        	
        	// predict the label
        	
        	int[] predLabel = new int[1];
            double[] confidence = new double[1];
            int result = -1;
            
            
            // Imgcodecs.imwrite("IMG-to-predict" + (ind++) + ".png", currentFace);
//            FaceRecognizer faceRecognizer = Face.createFisherFaceRecognizer();
            // Create a face recognizer. We need to specify the threshold so that
            // unknown faces would return '-1' label
            FaceRecognizer faceRecognizer = Face.createFisherFaceRecognizer(0,1500);
            faceRecognizer.load("traineddata");
        	faceRecognizer.predict(currentFace,predLabel,confidence);
//        	result = faceRecognizer.predict_label(currentFace);
        	result = predLabel[0];
        	
        	return new double[] {result,confidence[0]};
	}
	
	
	/**
	 * Method for face detection and tracking
	 * 
	 * @param frame
	 *            it looks for faces in this frame
	 */
	private void detectAndDisplay(Mat frame)
	{
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();
		
		// convert the frame in gray scale
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);
		
		// compute minimum face size (20% of the frame height, in our case)
		if (this.absoluteFaceSize == 0)
		{
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0)
			{
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}
		
		// detect faces
		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
				
		// each rectangle in faces is a face: draw them!
		Rect[] facesArray = faces.toArray(); 
		for (int i = 0; i < facesArray.length; i++) {
			Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);

			// Crop the detected faces
			Rect rectCrop = new Rect(facesArray[i].tl(), facesArray[i].br());
			Mat croppedImage = new Mat(frame, rectCrop);
			// Change to gray scale
			Imgproc.cvtColor(croppedImage, croppedImage, Imgproc.COLOR_BGR2GRAY);
			// Equalize histogram
			Imgproc.equalizeHist(croppedImage, croppedImage);
			// Resize the image to a default size
			Mat resizeImage = new Mat();
			Size size = new Size(250,250);
			Imgproc.resize(croppedImage, resizeImage, size);
			// Imgcodecs.imwrite("resources/trainingset/igor/250x250/1-igor_" + (index++) + ".png", resizeImage);
//			int prediction = faceRecognition(resizeImage);
			double[] returnedResults = faceRecognition(resizeImage);
			double prediction = returnedResults[0];
			double confidence = returnedResults[1];
			
//			System.out.println("PREDICTED LABEL IS: " + prediction);
			int label = (int) prediction;
			String name = "";
			if (names.containsKey(label)) {
				name = names.get(label);
			} else {
				name = "Unknown";
			}
			
			// Create the text we will annotate the box with:
//            String box_text = "Prediction = " + prediction + " Confidence = " + confidence;
            String box_text = "Prediction = " + name + " Confidence = " + confidence;
            // Calculate the position for annotated text (make sure we don't
            // put illegal values in there):
            double pos_x = Math.max(facesArray[i].tl().x - 10, 0);
            double pos_y = Math.max(facesArray[i].tl().y - 10, 0);
            // And now put it into the image:
            Imgproc.putText(frame, box_text, new Point(pos_x, pos_y), 
            		Core.FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 255, 0, 2.0));
		}
	}

	
//	public void collectTrainingData() {
//		//TODO finish the collector
//		// Button to start collecting data automatically
//		// each rectangle in faces is a face: draw them!
//		Rect[] facesArray = faces.toArray(); 
//		for (int i = 0; i < facesArray.length; i++) {
//			Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
//		// Crop the detected faces
//		Rect rectCrop = new Rect(facesArray[i].tl(), facesArray[i].br());
//		Mat croppedImage = new Mat(frame, rectCrop);
//		// Resize the image to a default size
//		Mat resizeImage = new Mat();
//		Size size = new Size(250,250);
//		Imgproc.resize(croppedImage, resizeImage, size);
//		// Save the cropped screenshot of the detected face
//		// Imgcodecs.imwrite("resources/trainingset/igor/250x250/1-igor_" + (index++) + ".png", resizeImage);
//		// Imgcodecs.imwrite("resources/trainingset/richard/250x250/2-richard_" + (index++) + ".png", resizeImage);
//					
//	}



	/**
	 * The action triggered by selecting the Haar Classifier checkbox. It loads
	 * the trained set to be used for frontal face detection.
	 */
	@FXML
	protected void haarSelected(Event event)
	{
		// check whether the lpb checkbox is selected and deselect it
		if (this.lbpClassifier.isSelected())
			this.lbpClassifier.setSelected(false);
			
		this.checkboxSelection("resources/haarcascades/haarcascade_frontalface_alt.xml");
	}
	
	/**
	 * The action triggered by selecting the LBP Classifier checkbox. It loads
	 * the trained set to be used for frontal face detection.
	 */
	@FXML
	protected void lbpSelected(Event event)
	{
		// check whether the haar checkbox is selected and deselect it
		if (this.haarClassifier.isSelected())
			this.haarClassifier.setSelected(false);
			
		this.checkboxSelection("resources/lbpcascades/lbpcascade_frontalface.xml");
	}
	
	/**
	 * Method for loading a classifier trained set from disk
	 * 
	 * @param classifierPath
	 *            the path on disk where a classifier trained set is located
	 */
	private void checkboxSelection(String classifierPath)
	{
		// load the classifier(s)
		this.faceCascade.load(classifierPath);
		
		// now the video capture can start
		this.cameraButton.setDisable(false);
	}
	
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame)
	{
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer, according to the PNG format
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
	
}
