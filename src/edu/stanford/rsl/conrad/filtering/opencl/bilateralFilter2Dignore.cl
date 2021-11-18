/*
 * Copyright (C) 2018 Jennifer Maier
 * CONRAD is developed as an Open Source project under the GNU General Public License (GPL).
 */

#define POS(x, y) ((x) + ((y) * width))

float computeEuclidianDistance(float i, float j, float x, float y){
	return sqrt((i-x)*(i-x) + (j-y)*(j-y));
}
 
float computeGeometricCloseness(float sigma_d, float i, float j, float x, float y){
	float d = computeEuclidianDistance(i,j,x,y) / sigma_d;
	return exp(-0.5 * d * d);
}

float computePhotometricDistance(float sigma_r, float val1, float val2){
	float d = fabs(val1 - val2) / sigma_r;
	return exp(-0.5 * d * d);
}

kernel void bilateralFilter2Dignore(global float * image, global float * out, int width, int height, int kernelWidth, int kernelHeight, float sigmaD, float sigmaR)	
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
	
	float sumWeight = 0.0;
	float sumFilter = 0.0;
	for (int i = gidX - kernelWidth/2; i <= gidX + kernelWidth/2; i++) {
		for (int j = gidY - kernelHeight/2; j <= gidY + kernelHeight/2; j++) {
		
			if (i >= 0 && i < width && j >= 0 && j < height) {
				if (!isnan(image[POS(i,j)])) {
		
					double currentWeight = computePhotometricDistance(sigmaR, image[POS(gidX,gidY)] , image[POS(i,j)]) * computeGeometricCloseness(sigmaD, i, j, gidX, gidY);

					sumWeight += currentWeight;
					sumFilter += currentWeight * image[POS(i,j)];
					
				}
			}
		}
	}
	out[POS(gidX, gidY)] = sumFilter / sumWeight;
	return;
}

