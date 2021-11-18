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
 * Tool for OpenCL Filtering of a Grid 2D with an arbitrary 3x3 Filter Kernel.
 * The values in the kernel should sum up to 1.
 * This tool uses the CONRAD internal Grid 2-D data structure.
 * 
 * @author Jennifer Maier
 *
 */
public class GaussAnisotropicFiltering2DToolignore extends OpenCLFilteringTool2D {

	private static final long serialVersionUID = 2381806512446848130L;
	protected int sigmaX = 1;
	protected int sigmaY = 1;
	protected int kernelWidth = 3;
	protected int kernelHeight = 3;
	protected CLBuffer<FloatBuffer> kernelBuffer;
	
	public GaussAnisotropicFiltering2DToolignore() {
		this.kernelName = kernelname.GAUSS_FILTER_ANISOTROPIC_2D_IGNORE;
	}
	
	@Override
	public void configure() throws Exception {		

		sigmaX = UserUtil.queryInt("Enter sigmaX (> 0)", sigmaX);
		if (sigmaX <= 0) {
			throw new IllegalArgumentException("Sigma needs to be > 0.");
		}
		
		sigmaY = UserUtil.queryInt("Enter sigmaY (> 0)", sigmaY);
		if (sigmaY <= 0) {
			throw new IllegalArgumentException("Sigma needs to be > 0.");
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
		
		// prepare kernel and buffer
		Grid2D kernelGrid = gaussianAnisotropicKernel(sigmaX, sigmaY);
//		kernelGrid.show("kernelGrid Anisotropic Gaussian");
//		
//		double kernelSum = 0.0;
//		for (int i = 0; i < kernelGrid.getSize()[0]; i++) {
//			for (int j = 0; j < kernelGrid.getSize()[1]; j++) {
//				kernelSum += kernelGrid.getAtIndex(i, j);
//			}
//		}
//		System.out.println(kernelSum);
		
		kernelWidth = kernelGrid.getWidth();
		kernelHeight = kernelGrid.getHeight();
		kernelBuffer = clContext.createFloatBuffer(kernelWidth * kernelHeight, CLMemory.Mem.READ_ONLY);
		gridToBuffer(kernelBuffer.getBuffer(), kernelGrid);
		kernelBuffer.getBuffer().rewind();
		queue.putWriteBuffer(kernelBuffer, true);
//		for (int i = 0; i < 7*19; i++) {
//			System.out.print(kernelBuffer.getBuffer().get(i) + ",");
//		}
//		System.out.println();
	}
	
	// Getter and Setter

	public void setConfigured(boolean configured) {
		this.configured = configured;
	}
	
	public void setSigmaX(int sigmaX) {
		this.sigmaX = sigmaX;
	}
	
	public int getSigmaX() {
		return this.sigmaX;
	}
	
	public void setSigmaY(int sigmaY) {
		this.sigmaY = sigmaY;
	}
	
	public int getSigmaY() {
		return this.sigmaY;
	}
	
	@Override
	protected void configureKernel() {
		filterKernel = program.createCLKernel("gaussAnisotropicFilter2Dignore");

		filterKernel
		.putArg(image)
		.putArg(result)
		.putArg(width)
		.putArg(height)
		.putArg(kernelWidth)
		.putArg(kernelHeight)
		.putArg(kernelBuffer);	

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
		return "OpenCL Gauss Anisotropic Filter 2D ignore";
	}

	@Override
	public ImageFilteringTool clone() {
		GaussAnisotropicFiltering2DToolignore clone = new GaussAnisotropicFiltering2DToolignore();
		clone.sigmaX = this.sigmaX;
		clone.sigmaY = this.sigmaY;
		clone.setConfigured(this.configured);
		return clone;
	}
	
	public static double gaussAnisotropic2D(double x, double y, double sigmaX, double sigmaY) {
		return 1/(2*Math.PI*sigmaX*sigmaY) * Math.exp( -0.5* ( ((x*x)/(sigmaX*sigmaX)) + ((y*y)/(sigmaY*sigmaY)) ) );
	}
	
	public static Grid2D gaussianAnisotropicKernel(double sigmaX, double sigmaY) {
		int width = (int) (2 * 3 * sigmaX + 1);
		int height = (int) (2 * 3 * sigmaY + 1);
		Grid2D kernel = new Grid2D(width, height);
		kernel.setSpacing(1.0f, 1.0f);
		kernel.setOrigin(0,0);
		
		for (int i = 0; i < kernel.getWidth(); i++) {
			for (int j = 0; j < kernel.getHeight(); j++) {
				double x = i - (int) (kernel.getWidth()/2);
				double y = j - (int) (kernel.getHeight()/2);
				
				kernel.setAtIndex(i, j, (float) gaussAnisotropic2D(x, y, sigmaX, sigmaY));
			}
		}
		
		return kernel;
		
		
	}
	
}

/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/