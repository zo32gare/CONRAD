/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
 */
 #define POS(x, y) ((x) + ((y) * width))
 
kernel void meanFilter2Dignore(global float * image, global float * out, int width, int height, int kernelWidth, int kernelHeight, float factor)
{
	int gidX = get_global_id(0);
	int gidY = get_global_id(1);
	
	// Make sure that coordinates are in range
	if (gidX < 0 || gidY < 0 ||	gidX > width-1 || gidY > height-1)
	{
		return;
	}
	
	// Make sure to not process NaNs
	if (isnan(image[POS(gidX,gidY)]))
	{
		return;
	}
	
	// sum up over kernel
	float sum = 0.0;
	float number = 0.0;
	for (int i = gidX - kernelWidth/2; i <= gidX + kernelWidth/2; i++) {
		for (int j = gidY - kernelHeight/2; j <= gidY + kernelHeight/2; j++) {
		
			if (i >= 0 && i < width && j >= 0 && j < height) {
				if (!isnan(image[POS(i,j)])) {
					sum += image[POS(i,j)];
					number += 1;
				}
			}

			
		
		}
	}
	
	out[POS(gidX, gidY)] = sum / number; 
	return;
}

