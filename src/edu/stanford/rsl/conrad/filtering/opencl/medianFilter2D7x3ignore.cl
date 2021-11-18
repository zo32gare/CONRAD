/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
 */
 #define POS(x, y) ((x) + ((y) * width))
 
kernel void medianFilter2D7x3ignore(global float * image, global float * out, int width, int height, int kernelWidth, int kernelHeight)
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
	
	// make sorted list of values in kernel range
	float list[21];
	int counter = 0;
	int numberOfEntries = 0;
	for (int i = gidX - kernelWidth/2; i <= gidX + kernelWidth/2; i++) {
		for (int j = gidY - kernelHeight/2; j <= gidY + kernelHeight/2; j++) {
			
			if (i >= 0 && i < width && j >= 0 && j < height) {
				if (!isnan(image[POS(i,j)])) {
					
					numberOfEntries++;
					float val = image[POS(i, j)];
					
					if (counter == 0) {
						list[counter++] = val;
					} else {
						int setVal = 0;
						for (int c = 0; c < counter; c++) {
							
							if (val < list[c] && setVal == 0) {
								for (int n = counter; n > c; n--) {
									list[n] = list[n-1];
								}
								list[c] = val;
								setVal = 1;
							}				
						}
						
						if (setVal == 0) {
							list[counter] = val; 
						}
						counter++;
					}
				}
			}
		
		}
	}
		
	out[POS(gidX, gidY)] = list[numberOfEntries/2]; 
	return;
}

