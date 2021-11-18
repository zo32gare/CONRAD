/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
*/
package edu.stanford.rsl.conrad.filtering.opencl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLProgram;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.filtering.ImageFilteringTool;
import edu.stanford.rsl.conrad.opencl.OpenCLUtil;
import edu.stanford.rsl.conrad.utils.UserUtil;


	/**
	 * This is an abstract template for a filtering tool based on OpenCL. It already implements basic procedures,
	 * which can be overwritten by subclasses.  
	 * @author Benedikt Lorch, Jennifer Maier
	 *
	 */
	public abstract class OpenCLFilteringTool2D extends ImageFilteringTool {

		private static final long serialVersionUID = -2903946802060212554L;		

		protected final int debug = 0;
		
		// initialized in init method.
		protected CLContext clContext;
		protected CLDevice device;
		protected CLKernel filterKernel;
		protected CLProgram program;
		protected boolean init = false;
		protected CLBuffer<FloatBuffer> image;
		protected CLBuffer<FloatBuffer> result;
		protected int width;
		protected int height;
		protected kernelname kernelName;
		
		public enum kernelname{
			KERNEL3x3_FILTER_2D("kernel3x3Filter2D.cl"),
			MEAN_FILTER_2D("meanFilter2D.cl"),
			MEAN_FILTER_2D_IGNORE("meanFilter2Dignore.cl"),
			BINARIZATION_2D("binarization2D.cl"),
			MEDIAN_FILTER_2D_3x3("medianFilter2D3x3.cl"),
			MEDIAN_FILTER_2D_3x3_IGNORE("medianFilter2D3x3ignore.cl"),
			MEDIAN_FILTER_2D_7x3("medianFilter2D7x3.cl"),
			MEDIAN_FILTER_2D_7x3_IGNORE("medianFilter2D7x3ignore.cl"),
			GAUSS_FILTER_2D("gaussFilter2D.cl"),
			GAUSS_FILTER_2D_IGNORE("gaussFilter2Dignore.cl"),
			GAUSS_FILTER_ANISOTROPIC_2D_IGNORE("gaussAnisotropicFilter2Dignore.cl"),
			BILATERAL_FILTER_2D_IGNORE("bilateralFilter2Dignore.cl");
			
			private String filename;
			
			private kernelname(String fn){
				this.filename = fn;
			}
			public String getName(){
				return this.filename;
			}
		}
		
		
		/**
		 * Initializes input and output buffer, clContext, dimensions and builds the OpenCL program
		 * @param width of the grids to process
		 * @param height of the grids to process
		 */
		@SuppressWarnings("unused")
		public void init(int width, int height) {
			if (!init) {
				// Get dimensions from given grid
				this.width = width;
				this.height = height;
				
				// Create OpenCL context and device if not done so yet
				getDevice();
				
				// Allocate memory
				image = clContext.createFloatBuffer(width * height, CLMemory.Mem.READ_ONLY);
				result = clContext.createFloatBuffer(width * height, CLMemory.Mem.WRITE_ONLY);
				
				// Build OpenCL program and create kernel
				try {
					InputStream input = OpenCLFilteringTool2D.class.getResourceAsStream(kernelName.filename);
					if (null == input) {
						throw new IOException("InputStream input is null." + kernelName.filename + " could not be loaded");
					}
					
					program = clContext.createProgram(input);
					program.build();
				} catch (IOException e) {
					throw new RuntimeException("The kernel file " + kernelName.filename + " could not be loaded.", e);
				}
				
				if (debug > 0) {
					System.out.println(program.getBuildStatus(device));
					System.out.println(program.getBuildLog());
				}
				
				init = true;
			}
		}
		
		/**
		 * Returns the device to be used. In case context or device have not been initalized yet, they will be created 
		 * @return device to be used
		 */
		public CLDevice getDevice() {
			if (null == device) {
				if (null == clContext) {
					clContext = OpenCLUtil.createContext();
					OpenCLUtil.initFilter(clContext);
				}
				device = clContext.getMaxFlopsDevice();
			}
			return device;
		}

		
		/**
		 * Configures the filter kernel
		 * Must set filterKernel = program.createCLKernel({kernelName})
		 * Must do filterKernel.putArg(..) in order of kernel's parameters
		 */
		protected abstract void configureKernel();
		
		
		/**
		 * Called by process() before the processing begins. Put your write buffers to the queue here.
		 * @param input Grid 2-D to be processed
		 * @param queue CommandQueue for the specific device
		 */
		protected void prepareProcessing(Grid2D input, CLCommandQueue queue) {
			// Copy image data into linear floatBuffer
			gridToBuffer(image.getBuffer(), input);
			image.getBuffer().rewind();
			queue.putWriteBuffer(image, true);
		}
		
		
		/**
		 * Called by process() after the processing. Put your read buffers to the queue here.
		 * @param queue CommandQueue for the specific device
		 * @return result as Grid 2-D
		 */
		@SuppressWarnings("unused")
		protected Grid2D completeProcessing(CLCommandQueue queue) {
			// Read back result with blocking call
			queue.putReadBuffer(result, true);
			
			if (debug > 0) {
				System.out.println("Retrieved the result buffer back from the GPU");
			}
			
			// Copy the result from the buffer back into a Grid2D
			FloatBuffer resultBuffer = result.getBuffer();
			resultBuffer.rewind();
			Grid2D result = bufferToGrid(resultBuffer, width, height);		
			resultBuffer.rewind();
			
			if (debug > 0) {
				System.out.println("Transformed the result buffer back to a Grid 2-D");
			}
			return result;
		}
		
		
		/**
		 * Applies the kernel to the given grid
		 * @param grid 2-D image to filter
		 * @return the filtered 2-D image
		 */
		@SuppressWarnings("unused")
		public Grid2D process(Grid2D grid) {
			try {
				int size[] = grid.getSize();
				// If not initialized yet, it's getting time
				if (!init) {
					if (this.kernelName == null) { // shouldn't happen
						ArrayList<kernelname> availableKernelsList = new ArrayList<kernelname>(EnumSet.allOf(kernelname.class));
						kernelname[] availableKernelsArray = availableKernelsList.toArray(new kernelname[availableKernelsList.size()]);
						this.kernelName = (kernelname) UserUtil.chooseObject("Please select the 3D openCL filter you would like to apply", "Choose Filter", availableKernelsArray, kernelname.BINARIZATION_2D);
					}
					init(size[0], size[1]);
				}				
				
				// Make sure that dimensions do match
				if (width != size[0] || height != size[1]) {
					throw new IllegalArgumentException("The given grid's dimensions are not equal to the sizes which this filter has been configured for.");
				}
				
				CLCommandQueue queue = device.createCommandQueue();
				
				// Copy image data into linear floatBuffer
				prepareProcessing(grid, queue);
		
				// Create filter kernel and put args
				configureKernel();
				
				// GPU dependent constraints:
				// localWorkSizeX * localWorkSizeY * localWorkSizeZ <= kernel.getWorkGroupSize(device)
				// Each individual dimension <= device.getMaxWorkItemSizes()
				long workGroupSize = filterKernel.getWorkGroupSize(device);
				
				// Cube root will usually not result in integer
				int groupSize = (int) Math.ceil(Math.pow(workGroupSize, 1.0/2));
				groupSize = (groupSize * groupSize > workGroupSize) ? groupSize - 1 : groupSize;
				
				if (debug > 0) {
					System.out.println("Starting processing on GPU");
				}
				
				// Enqueue the filter kernel
				queue.put2DRangeKernel(filterKernel, 0, 0, OpenCLUtil.roundUp(groupSize, width), OpenCLUtil.roundUp(groupSize, height), groupSize, groupSize);
				queue.flush();
				queue.finish();
				
				if (debug > 0) {
					System.out.println("Finished processing on GPU");
				}
				
				Grid2D result = completeProcessing(queue);
				
				queue.release();
				return result;
			
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			return null;
		}
		
		/**
		 * Copies the float data of a given grid into a linear float buffer
		 * @param imageBuffer: destination buffer
		 * @param grid: input 2-D grid
		 * @return linear float buffer
		 */
		protected FloatBuffer gridToBuffer(FloatBuffer imageBuffer, Grid2D grid) {
			imageBuffer.rewind();
			float[] planeBuffer = grid.getBuffer();
			imageBuffer.put(planeBuffer);

			return imageBuffer;
		}
		
		
		/**
		 * Copies the float buffer as linear memory into a Grid 2-D
		 * @param FloatBuffer buffer as linear memory
		 * @param width of the 2-D grid to be returned
		 * @param height of the 2-D grid to be returned
		 * @return Grid2D
		 */
		protected Grid2D bufferToGrid(FloatBuffer buffer, int width, int height) {

			buffer.rewind();	
			float[] plane = new float[width * height];	
			buffer.get(plane);
			Grid2D grid = new Grid2D(plane, width, height);

			return grid;
		}
		
		public void setKernelName(kernelname kernelName) {
			this.kernelName = kernelName;
		}
		
		public kernelname getKernelName() {
			return this.kernelName;
		}

		@Override
		public void prepareForSerialization() {
			init = false;
		}
		
		
		public void cleanup(){
			release();
		}

		
		protected void release(){
			image.release();
			result.release();
			filterKernel.release();
			OpenCLUtil.releaseContext(clContext);
		}
		
	}
	/*
	 * Copyright (C) 2018 Jennifer Maier
	 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
	*/