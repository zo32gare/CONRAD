/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
 */
 #define POS(x, y) ((x) + ((y) * width))
 
kernel void medianFilter2D7x3(global float * image, global float * out, int width, int height, int kernelWidth, int kernelHeight)
{
	int gidX = get_global_id(0);
	int gidY = get_global_id(1);
	
	// Make sure that coordinates are in range
	if (gidX < kernelWidth/2 || gidY < kernelHeight/2 ||
		gidX > width-kernelWidth/2-1 || gidY > height-kernelHeight/2-1)
	{
		return;
	}
	
	// make sorted list of values in kernel range
	float list[21];
	int counter = 0;
	for (int i = gidX - kernelWidth/2; i <= gidX + kernelWidth/2; i++) {
		for (int j = gidY - kernelHeight/2; j <= gidY + kernelHeight/2; j++) {
			
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
		
	out[POS(gidX, gidY)] = list[(kernelWidth*kernelHeight)/2]; 
	return;
}

