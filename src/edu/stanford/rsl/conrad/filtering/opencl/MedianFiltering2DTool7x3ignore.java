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
import edu.stanford.rsl.conrad.opencl.OpenCLUtil;
import edu.stanford.rsl.conrad.utils.UserUtil;

/**
 * Tool for OpenCL 2D Median Filtering a Grid 2D.
 * This tool uses the CONRAD internal Grid 2-D data structure.
 *   
 * @author Jennifer Maier
 *
 */
public class MedianFiltering2DTool7x3ignore extends OpenCLFilteringTool2D {

	private static final long serialVersionUID = 998903543289777965L;
	protected int kernelWidth = 7;
	protected int kernelHeight = 3;
	protected CLBuffer<FloatBuffer> listBuffer;
	
	public MedianFiltering2DTool7x3ignore() {
		this.kernelName = kernelname.MEDIAN_FILTER_2D_7x3_IGNORE;
	}

	@Override
	public void configure() throws Exception {		
		
		configured = true;

	}
	
	// Getter and Setter
	public void setConfigured(boolean configured) {
		this.configured = configured;
	}
	
	public int getKernelWidth() {
		return kernelWidth;
	}

	public void setKernelWidth(int kernelWidth) {
		if (kernelWidth % 2 == 0) {
			kernelWidth++;
		}
		this.kernelWidth = kernelWidth;
	}

	public int getKernelHeight() {
		return kernelHeight;
	}

	public void setKernelHeight(int kernelHeight) {
		if (kernelHeight % 2 == 0) {
			kernelHeight++;
		}
		this.kernelHeight = kernelHeight;
	}

	@Override
	protected void configureKernel() {
		filterKernel = program.createCLKernel("medianFilter2D7x3ignore");
		
		filterKernel
		.putArg(image)
		.putArg(result)
		.putArg(width)
		.putArg(height)
		.putArg(kernelWidth)
		.putArg(kernelHeight);		
		
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
		return "OpenCL 7x3 Median Filter 2D ignore";
	}

	@Override
	public ImageFilteringTool clone() {
		MeanFiltering2DTool clone = new MeanFiltering2DTool();
		clone.setKernelHeight(this.getKernelHeight());
		clone.setKernelWidth(this.getKernelWidth());
		clone.setConfigured(this.configured);

		return clone;
	}
	
}

/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/