/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/
package edu.stanford.rsl.conrad.filtering.opencl;

import java.nio.FloatBuffer;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLMemory;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.filtering.ImageFilteringTool;
import edu.stanford.rsl.conrad.utils.UserUtil;

/**
 * Tool for OpenCL Filtering of a Grid 2D with a Bilateral Filter Kernel.
 * This tool uses the CONRAD internal Grid 2-D data structure.
 * 
 * @author Jennifer Maier
 *
 */
public class BilateralFiltering2DToolignore extends OpenCLFilteringTool2D {

	private static final long serialVersionUID = 2381806512446848130L;
	protected double sigmaS = 1;
	protected double sigmaR = 1;
	protected int kernelWidth = 3;
	protected int kernelHeight = 3;
	protected CLBuffer<FloatBuffer> kernelBuffer;
	
	public BilateralFiltering2DToolignore() {
		this.kernelName = kernelname.BILATERAL_FILTER_2D_IGNORE;
	}
	
	@Override
	public void configure() throws Exception {		

		sigmaS = UserUtil.queryDouble("Enter sigmaS (> 0)", sigmaS);
		if (kernelWidth <= 0) {
			throw new IllegalArgumentException("SigmaS needs to be > 0.");
		}
		
		sigmaR = UserUtil.queryDouble("Enter sigmaR (between 0 and 1)", sigmaR);
		if (kernelWidth <= 0) {
			throw new IllegalArgumentException("SigmaR needs to be > 0.");
		}
		
		configured = true;

	}
	
	/**
	 * Called by process() before the processing begins. Put your write buffers to the queue here.
	 * @param input Grid 3-D to be processed
	 * @param queue CommandQueue for the specific device
	 */
	@Override
	protected void prepareProcessing(Grid2D input, CLCommandQueue queue) {
		
		// Copy image data into linear floatBuffer
		gridToBuffer(image.getBuffer(), input);
		image.getBuffer().rewind();
		queue.putWriteBuffer(image, true);
		
//		// prepare kernel and buffer
//		Grid2D kernelGrid = gaussianKernel(sigmaS);
//		kernelWidth = kernelGrid.getWidth();
//		kernelHeight = kernelGrid.getHeight();
//		kernelBuffer = clContext.createFloatBuffer(kernelWidth * kernelHeight, CLMemory.Mem.READ_ONLY);
//		gridToBuffer(kernelBuffer.getBuffer(), kernelGrid);
//		kernelBuffer.getBuffer().rewind();
//		queue.putWriteBuffer(kernelBuffer, true);
	}
	
	// Getter and Setter

	public void setConfigured(boolean configured) {
		this.configured = configured;
	}
	
	public void setSigmaS(double sigmaS) {
		this.sigmaS = sigmaS;
	}
	
	public double getSigmaS() {
		return this.sigmaS;
	}
	
	public void setSigmaR(double sigmaR) {
		this.sigmaR = sigmaR;
	}
	
	public double getSigmaR() {
		return this.sigmaR;
	}
	
	@Override
	protected void configureKernel() {
		filterKernel = program.createCLKernel("bilateralFilter2Dignore");

		filterKernel
		.putArg(image)
		.putArg(result)
		.putArg(width)
		.putArg(height)
		.putArg(kernelWidth)
		.putArg(kernelHeight)
		.putArg((float)sigmaS)
		.putArg((float)sigmaR);	

	}

	@Override
	public String getBibtexCitation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMedlineCitation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDeviceDependent() {
		return true;
	}

	@Override
	public String getToolName() {
		return "OpenCL Bilateral Filter 2D ignore";
	}

	@Override
	public ImageFilteringTool clone() {
		BilateralFiltering2DToolignore clone = new BilateralFiltering2DToolignore();
		clone.sigmaS = this.sigmaS;
		clone.sigmaR = this.sigmaR;
		clone.setConfigured(this.configured);
		return clone;
	}
	
	public static double gauss2D(double x, double y, double sigma) {
		return 1/(2*Math.PI*Math.pow(sigma, 2)) * Math.exp(-(x*x + y*y)/(2*Math.pow(sigma, 2)));
	}
	
	public static Grid2D gaussianKernel(double sigma) {
		int width = (int) (2 * 3 * sigma + 1);
		Grid2D kernel = new Grid2D(width, width);
		kernel.setSpacing(1.0f, 1.0f);
		kernel.setOrigin(0,0);
		
		for (int i = 0; i < kernel.getWidth(); i++) {
			for (int j = 0; j < kernel.getHeight(); j++) {
				double x = i - (int) (kernel.getWidth()/2);
				double y = j - (int) (kernel.getHeight()/2);
				
				kernel.setAtIndex(i, j, (float) gauss2D(x, y, sigma));
			}
		}
		
		return kernel;
		
		
	}
	
}

/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/